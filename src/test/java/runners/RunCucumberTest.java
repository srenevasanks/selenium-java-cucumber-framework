package runners;

import framework.config.Config;
import io.cucumber.testng.*;
import org.testng.annotations.*;

@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"hooks", "stepdefinitions"},
    plugin = {
      "pretty",
      "json:target/test-output/cucumber.json",
      "html:target/test-output/cucumber.html",
      "reporting.ExtentCucumberReporter"
    },
    monochrome = true)
public final class RunCucumberTest {
  private TestNGCucumberRunner runner;

  @BeforeClass(alwaysRun = true)
  public void setup() {
    runner = new TestNGCucumberRunner(getClass());
  }

  @Test(dataProvider = "scenarios")
  public void runScenario(PickleWrapper pickle, FeatureWrapper feature) {
    runner.runScenario(pickle.getPickle());
  }

  @DataProvider
  public Object[][] scenarios() {
    return Config.bool("scenarios.parallel") ? new Object[0][0] : runner.provideScenarios();
  }

  @Test(dataProvider = "parallelScenarios")
  public void runParallelScenario(PickleWrapper pickle, FeatureWrapper feature) {
    runner.runScenario(pickle.getPickle());
  }

  @DataProvider(parallel = true)
  public Object[][] parallelScenarios() {
    return Config.bool("scenarios.parallel") ? runner.provideScenarios() : new Object[0][0];
  }

  @AfterClass(alwaysRun = true)
  public void teardown() {
    if (runner != null) runner.finish();
  }
}
