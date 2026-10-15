package framework.driver;

import framework.config.Config;
import java.net.URI;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class DriverManager {
  private static final ThreadLocal<WebDriver> DRIVERS = new ThreadLocal<>();

  private DriverManager() {}

  public static void start() {
    if (!Config.get("browser").equalsIgnoreCase("chrome"))
      throw new IllegalArgumentException("Only Chrome is supported");
    if (DRIVERS.get() != null) throw new IllegalStateException("Driver already initialized");
    ChromeOptions options = new ChromeOptions();
    if (Config.bool("headless")) options.addArguments("--headless=new");
    options.addArguments("--window-size=1440,1000", "--disable-dev-shm-usage");
    if (!Config.get("chrome.binary").isBlank()) options.setBinary(Config.get("chrome.binary"));
    WebDriver driver;
    if (Config.get("remote.url").isBlank()) driver = new ChromeDriver(options);
    else {
      try {
        driver = new RemoteWebDriver(URI.create(Config.get("remote.url")).toURL(), options);
      } catch (java.net.MalformedURLException e) {
        throw new IllegalArgumentException("Invalid remote.url", e);
      }
    }
    DRIVERS.set(driver);
    driver
        .manage()
        .timeouts()
        .pageLoadTimeout(Duration.ofSeconds(Config.integer("pageLoad.seconds")));
    driver.manage().timeouts().implicitlyWait(Duration.ZERO);
  }

  public static WebDriver get() {
    WebDriver driver = DRIVERS.get();
    if (driver == null) throw new IllegalStateException("No driver on current scenario thread");
    return driver;
  }

  public static boolean exists() {
    return DRIVERS.get() != null;
  }

  public static void stop() {
    try {
      if (exists()) get().quit();
    } finally {
      DRIVERS.remove();
    }
  }
}
