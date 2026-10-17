package hooks;

import framework.driver.DriverManager;
import io.cucumber.java.*;
import java.nio.file.*;
import java.util.UUID;
import org.openqa.selenium.*;
import org.slf4j.*;

public final class BrowserHooks {
  private static final Logger LOG = LoggerFactory.getLogger(BrowserHooks.class);
  private boolean failureCaptured;

  @Before
  public void start(Scenario scenario) {
    LOG.info("Starting scenario: {}", scenario.getName());
    DriverManager.start();
    var caps = ((org.openqa.selenium.remote.RemoteWebDriver) DriverManager.get()).getCapabilities();
    scenario.log("Browser: " + caps.getBrowserName() + " " + caps.getBrowserVersion());
  }

  private void capture(Scenario scenario, String label) throws java.io.IOException {
    byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
    Files.createDirectories(Path.of("target/test-output/screenshots"));
    Files.write(Path.of("target/test-output/screenshots", UUID.randomUUID() + ".png"), png);
    scenario.attach(png, "image/png", label);
  }

  @After(order = 300)
  public void captureNetworkDiagnostics(Scenario scenario) throws java.io.IOException {
    if (Boolean.getBoolean("ci.diagnostics") && DriverManager.exists())
      CiNetworkDiagnostics.capture(scenario);
  }

  @After(order = 200)
  public void evidenceBeforeCleanup(Scenario scenario) throws java.io.IOException {
    if (!DriverManager.exists()) return;
    if (scenario.isFailed()) {
      capture(scenario, "Failure before cleanup");
      failureCaptured = true;
    } else if (scenario.getSourceTagNames().contains("@e2e")) {
      capture(scenario, "Completed customer journey");
    }
  }

  @After(order = 0)
  public void finish(Scenario scenario) throws java.io.IOException {
    try {
      if (scenario.isFailed() && !failureCaptured && DriverManager.exists())
        capture(scenario, "Failure screenshot");
    } finally {
      DriverManager.stop();
      LOG.info("Finished scenario: {} ({})", scenario.getName(), scenario.getStatus());
    }
  }
}
