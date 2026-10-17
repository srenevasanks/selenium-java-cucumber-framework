package reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import framework.config.Config;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.testng.*;
import org.testng.xml.XmlSuite;

/** Summarizes actual TestNG results without mixing component tests into Cucumber/Extent. */
public final class ExecutionSummaryReporter implements IReporter, IExecutionListener {
  private final Instant started = Instant.now();

  @Override
  public void onExecutionStart() {
    // A component-only invocation must not expose a stale Cucumber report from a previous run.
    try {
      Files.createDirectories(Path.of("target/test-output"));
      for (String name : List.of("cucumber-summary.json", "cucumber.json", "cucumber.html",
          "extent-report.html", "execution-summary.json")) {
        Files.deleteIfExists(Path.of("target/test-output", name));
      }
    } catch (IOException error) {
      throw new IllegalStateException("Cannot prepare runtime report directory", error);
    }
  }

  @Override
  public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
    Map<String, int[]> groups = new LinkedHashMap<>();
    groups.put("liveCucumber", new int[3]);
    groups.put("browserComponents", new int[3]);
    groups.put("otherTests", new int[3]);
    int configurationFailures = 0;
    int configurationSkips = 0;
    for (ISuite suite : suites) {
      for (ISuiteResult result : suite.getResults().values()) {
        ITestContext context = result.getTestContext();
        count(context.getPassedTests(), 0, groups);
        count(context.getFailedTests(), 1, groups);
        count(context.getSkippedTests(), 2, groups);
        configurationFailures += context.getFailedConfigurations().size();
        configurationSkips += context.getSkippedConfigurations().size();
      }
    }
    int[] total = new int[3];
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("startedUtc", started.toString());
    summary.put("finishedUtc", Instant.now().toString());
    summary.put("mode", Boolean.getBoolean("cucumber.execution.dry-run") ? "dry-run" : "browser-execution");
    summary.put("javaVersion", System.getProperty("java.version"));
    summary.put("os", System.getProperty("os.name"));
    summary.put("browser", Config.get("browser"));
    summary.put("headless", Config.bool("headless"));
    summary.put("parallel", Config.bool("scenarios.parallel"));
    summary.put("configuredWorkers", Config.integer("threads"));
    summary.put("tagFilter", System.getProperty("cucumber.filter.tags", ""));
    groups.forEach((name, counts) -> {
      summary.put(name, totals(counts));
      for (int i = 0; i < total.length; i++) total[i] += counts[i];
    });
    summary.put("totalTestMethods", totals(total));
    // Surefire can additionally include failed configuration invocations in its headline total.
    summary.put("configurationFailures", configurationFailures);
    summary.put("configurationSkips", configurationSkips);
    summary.put("source", "TestNG result sets; Extent remains Cucumber-only");
    summary.put("surefireDirectory", "target/surefire-reports");
    try {
      ObjectMapper mapper = new ObjectMapper();
      Path eventSummary = Path.of("target/test-output/cucumber-summary.json");
      if (groups.get("liveCucumber")[0] + groups.get("liveCucumber")[1]
          + groups.get("liveCucumber")[2] > 0 && Files.exists(eventSummary)) {
        var events = mapper.readTree(eventSummary.toFile());
        summary.put("cucumberEvents", events);
        int[] c = groups.get("liveCucumber");
        summary.put("cucumberCountsAgree", events.path("passed").asInt() == c[0]
            && events.path("failed").asInt() == c[1]
            && events.path("skipped").asInt() == c[2]);
      }
      mapper.writerWithDefaultPrettyPrinter().writeValue(
          Path.of("target/test-output/execution-summary.json").toFile(), summary);
    } catch (IOException error) {
      throw new IllegalStateException("Cannot write consolidated execution summary", error);
    }
  }

  private static void count(IResultMap results, int status, Map<String, int[]> groups) {
    for (ITestResult result : results.getAllResults()) {
      String owner = result.getTestClass().getName();
      String group = switch (owner) {
        case "runners.RunCucumberTest" -> "liveCucumber";
        case "components.AdvertisementTest" -> "browserComponents";
        default -> "otherTests";
      };
      groups.get(group)[status]++;
    }
  }

  private static Map<String, Integer> totals(int[] counts) {
    return Map.of("total", counts[0] + counts[1] + counts[2], "passed", counts[0],
        "failed", counts[1], "skipped", counts[2]);
  }
}
