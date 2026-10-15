package reporting;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import framework.config.Config;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExtentCucumberReporter implements ConcurrentEventListener {
  private final ExtentReports report = new ExtentReports();
  private final Map<UUID, ExtentTest> tests = new ConcurrentHashMap<>();
  private final Map<String, String> featureNames = new ConcurrentHashMap<>();
  private final AtomicInteger passed = new AtomicInteger(),
      failed = new AtomicInteger(),
      skipped = new AtomicInteger();
  private Instant started;
  private final AtomicInteger active = new AtomicInteger(), peakConcurrent = new AtomicInteger();

  @Override
  public void setEventPublisher(EventPublisher publisher) {
    publisher.registerHandlerFor(TestRunStarted.class, this::start);
    publisher.registerHandlerFor(TestCaseStarted.class, this::scenario);
    publisher.registerHandlerFor(
        TestSourceRead.class,
        event ->
            event
                .getSource()
                .lines()
                .map(String::strip)
                .filter(line -> line.startsWith("Feature:"))
                .findFirst()
                .ifPresent(
                    line ->
                        featureNames.put(
                            event.getUri().toString(),
                            line.substring("Feature:".length()).strip())));
    publisher.registerHandlerFor(TestStepFinished.class, this::step);
    publisher.registerHandlerFor(EmbedEvent.class, this::embed);
    publisher.registerHandlerFor(
        WriteEvent.class, event -> tests.get(event.getTestCase().getId()).info(event.getText()));
    publisher.registerHandlerFor(TestCaseFinished.class, this::finishScenario);
    publisher.registerHandlerFor(TestRunFinished.class, this::finish);
  }

  private void start(TestRunStarted event) {
    started = event.getInstant();
    try {
      Files.createDirectories(Path.of("reports"));
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
    var spark = new ExtentSparkReporter("reports/extent-report.html");
    spark.config().setDocumentTitle("Automation Exercise Automation Results");
    spark.config().setReportName("Automation Exercise | Cucumber execution");
    report.attachReporter(spark);
    report.setSystemInfo("Environment", Config.get("baseUrl"));
    report.setSystemInfo("Browser", Config.get("browser"));
    report.setSystemInfo("Headless", Config.get("headless"));
    report.setSystemInfo("Parallel scenarios", Config.get("scenarios.parallel"));
    report.setSystemInfo("Configured workers", Config.get("threads"));
    report.setSystemInfo(
        "Execution mode",
        Boolean.getBoolean("cucumber.execution.dry-run")
            ? "DRY RUN - no browser execution"
            : "Selenium UI execution");
    report.setSystemInfo("Java", System.getProperty("java.version"));
    report.setSystemInfo("Started UTC", started.toString());
  }

  private void scenario(TestCaseStarted event) {
    peakConcurrent.accumulateAndGet(active.incrementAndGet(), Math::max);
    String feature =
        featureNames.getOrDefault(
            event.getTestCase().getUri().toString(), event.getTestCase().getUri().getPath());
    ExtentTest test =
        report.createTest(
            event.getTestCase().getName(),
            "Feature: " + feature + ":" + event.getTestCase().getLocation().getLine());
    test.assignCategory(event.getTestCase().getTags().toArray(String[]::new));
    tests.put(event.getTestCase().getId(), test);
  }

  private void step(TestStepFinished event) {
    ExtentTest test = tests.get(event.getTestCase().getId());
    String name =
        event.getTestStep() instanceof PickleStepTestStep p
            ? p.getStep().getKeyword() + p.getStep().getText()
            : "Lifecycle hook";
    ExtentTest node = test.createNode(name);
    switch (event.getResult().getStatus()) {
      case PASSED -> node.pass("Duration: " + event.getResult().getDuration().toMillis() + " ms");
      case SKIPPED -> node.skip("Skipped because a preceding step or hook did not pass");
      default -> {
        if (event.getResult().getError() != null) node.fail(event.getResult().getError());
        else node.fail(event.getResult().getStatus().name());
      }
    }
  }

  private void embed(EmbedEvent event) {
    if ("image/png".equals(event.getMediaType()))
      tests
          .get(event.getTestCase().getId())
          .addScreenCaptureFromBase64String(
              Base64.getEncoder().encodeToString(event.getData()), event.getName());
  }

  private void finishScenario(TestCaseFinished event) {
    active.decrementAndGet();
    switch (event.getResult().getStatus()) {
      case PASSED -> passed.incrementAndGet();
      case SKIPPED -> skipped.incrementAndGet();
      default -> failed.incrementAndGet();
    }
  }

  private void finish(TestRunFinished event) {
    int total = passed.get() + failed.get() + skipped.get();
    double percent = total == 0 ? 0 : 100.0 * passed.get() / total;
    report.setSystemInfo("Total scenarios", "" + total);
    report.setSystemInfo("Observed peak concurrent scenarios", "" + peakConcurrent.get());
    report.setSystemInfo("Passed", "" + passed.get());
    report.setSystemInfo("Failed", "" + failed.get());
    report.setSystemInfo("Skipped", "" + skipped.get());
    report.setSystemInfo("Pass percentage", String.format(Locale.ROOT, "%.2f%%", percent));
    report.setSystemInfo(
        "Duration seconds", "" + Duration.between(started, event.getInstant()).toSeconds());
    report.flush();
    String summary =
        String.format(
            Locale.ROOT,
            "{\"started\":\"%s\",\"total\":%d,\"passed\":%d,\"failed\":%d,\"skipped\":%d,\"passPercentage\":%.2f,\"durationSeconds\":%d,\"parallel\":%s,\"configuredWorkers\":%d,\"peakConcurrentScenarios\":%d}%n",
            started,
            total,
            passed.get(),
            failed.get(),
            skipped.get(),
            percent,
            Duration.between(started, event.getInstant()).toSeconds(),
            Config.bool("scenarios.parallel"),
            Config.integer("threads"),
            peakConcurrent.get());
    try {
      Files.writeString(Path.of("reports/execution-summary.json"), summary);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
