package framework.pages;

import framework.components.Advertisement;
import framework.config.Config;
import framework.driver.DriverManager;
import java.net.URI;
import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public abstract class BasePage {
  protected final WebDriver driver = DriverManager.get();
  protected final WebDriverWait wait =
      new WebDriverWait(driver, Duration.ofSeconds(Config.integer("wait.seconds")));
  private final Advertisement advertisement = new Advertisement(driver);

  protected boolean pageIsClear() {
    advertisement.dismissVignette();
    return !advertisement.isVignetteActive();
  }

  protected WebElement visible(String css) {
    return wait.until(
        browser -> {
          if (!pageIsClear()) return null;
          return ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)).apply(browser);
        });
  }

  protected void click(String css) {
    wait.until(
        browser -> {
          if (!pageIsClear()) return false;
          WebElement element =
              ExpectedConditions.elementToBeClickable(By.cssSelector(css)).apply(browser);
          if (element == null) return false;
          if (!centerAndUncovered(element)) return false;
          try {
            element.click();
            return true;
          } catch (ElementClickInterceptedException intercepted) {
            // Only an observed vignette is recoverable here. Other interception remains a failure.
            if (!advertisement.isVignetteActive()) throw intercepted;
            return false;
          }
        });
    advertisement.awaitClear();
  }

  protected void fill(String css, String value) {
    WebElement field = wait.until(browser -> {
      if (!pageIsClear()) return null;
      WebElement candidate = ExpectedConditions.elementToBeClickable(By.cssSelector(css)).apply(browser);
      return candidate != null && centerAndUncovered(candidate) ? candidate : null;
    });
    field.clear();
    field.sendKeys(value);
  }

  private boolean centerAndUncovered(WebElement element) {
    return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("""
        const element = arguments[0];
        element.scrollIntoView({behavior:'instant', block:'center', inline:'nearest'});
        const rect = element.getBoundingClientRect();
        const x = Math.max(0, rect.left) +
            (Math.min(innerWidth, rect.right) - Math.max(0, rect.left)) / 2;
        const y = Math.max(0, rect.top) +
            (Math.min(innerHeight, rect.bottom) - Math.max(0, rect.top)) / 2;
        const hit = document.elementFromPoint(x, y);
        return rect.width > 0 && rect.height > 0 &&
            (hit === element || element.contains(hit));
        """, element));
  }

  protected void transition(String css, String path, String destination) {
    String sourceUrl = driver.getCurrentUrl().split("#", 2)[0];
    long previousDismissals = advertisement.dismissalCount();
    click(css);
    // A vignette can consume a link's first click instead of resuming its navigation.
    // Continue that same link once, only after a real dismissal on the unchanged source page.
    if (advertisement.dismissalCount() > previousDismissals
        && driver.getCurrentUrl().split("#", 2)[0].equals(sourceUrl)
        && !URI.create(sourceUrl).getPath().equals(path)) {
      var links = driver.findElements(By.cssSelector(css));
      if (!links.isEmpty() && "a".equalsIgnoreCase(links.get(0).getTagName())) click(css);
    }
    awaitPage(path, destination);
  }

  protected void expand(String css, String panel) {
    long previousDismissals = advertisement.dismissalCount();
    boolean[] continuedAfterAd = {false};
    click(css);
    wait.until(browser -> {
      if (!pageIsClear()) return false;
      if (displayed(panel)) return true;
      if (!continuedAfterAd[0] && advertisement.dismissalCount() > previousDismissals) {
        continuedAfterAd[0] = true;
        click(css);
      }
      return false;
    });
  }

  protected void awaitPage(String path, String destination) {
    wait.withMessage(() -> "Expected page " + path + " with " + destination
            + "; actual URL: " + driver.getCurrentUrl()).until(
        browser -> {
          return pageIsClear()
              && URI.create(browser.getCurrentUrl()).getPath().equals(path)
              && browser.findElements(By.cssSelector(destination)).stream()
                  .anyMatch(WebElement::isDisplayed);
        });
  }

  public void open(String path) {
    driver.get(Config.get("baseUrl") + path);
    awaitPage(path.isEmpty() ? "/" : path, "header a[href='/products']");
  }

  public String text(String css) {
    // Advertising annotations may insert non-breaking spaces inside otherwise identical labels.
    return visible(css).getText().replace('\u00a0', ' ').replaceAll("\\s+", " ").strip();
  }

  public String body() {
    return text("body");
  }

  public boolean displayed(String css) {
    return driver.findElements(By.cssSelector(css)).stream().anyMatch(WebElement::isDisplayed);
  }

  public boolean hasMissingValue(String css) {
    return Boolean.TRUE.equals(
        ((JavascriptExecutor) driver)
            .executeScript("return arguments[0].validity.valueMissing;", visible(css)));
  }

  public boolean hasEmailTypeMismatch(String css) {
    return Boolean.TRUE.equals(
        ((JavascriptExecutor) driver)
            .executeScript("return arguments[0].validity.typeMismatch;", visible(css)));
  }

  public String validationMessage(String css) {
    return visible(css).getDomProperty("validationMessage");
  }
}
