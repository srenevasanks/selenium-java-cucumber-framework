package framework.components;

import framework.config.Config;
import java.time.Duration;
import java.util.ArrayList;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles ordinary advertising controls only. Never interacts with security verification. */
public final class Advertisement {
  private static final Logger LOG = LoggerFactory.getLogger(Advertisement.class);
  private final WebDriver driver;
  private long dismissalCount;

  public Advertisement(WebDriver driver) {
    this.driver = driver;
  }

  public long dismissalCount() {
    return dismissalCount;
  }

  public boolean isVignetteActive() {
    if (!hasVignetteMarker()) return false;
    Dimension window = driver.manage().window().getSize();
    for (WebElement frame : driver.findElements(By.cssSelector("iframe[title='Advertisement']"))) {
      try {
        Rectangle bounds = frame.getRect();
        if (!frame.isDisplayed() || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) continue;
        // A large visible overlay can block the page while its Close control is still loading.
        boolean coveringPage =
            bounds.getWidth() >= window.getWidth() / 2
                && bounds.getHeight() >= window.getHeight() / 2;
        driver.switchTo().frame(frame);
        if (coveringPage || hasCloseControl(0)) return true;
      } catch (StaleElementReferenceException | NoSuchFrameException changedFrame) {
        LOG.debug("Advertisement changed while checking its visibility", changedFrame);
      } finally {
        driver.switchTo().defaultContent();
      }
    }
    // Google can leave its fragment in the URL after a same-page action has resumed.
    return false;
  }

  private boolean hasVignetteMarker() {
    return driver.getCurrentUrl().contains("#google_vignette");
  }

  /** Wait for the ad's real close control, then allow its pending navigation to resume. */
  public void awaitClear() {
    if (!isVignetteActive()) return;
    new WebDriverWait(driver, Duration.ofSeconds(Config.integer("wait.seconds")))
        .withMessage("Ordinary advertisement did not close through its exposed control")
        .until(
            browser -> {
              dismissVignette();
              return !isVignetteActive();
            });
  }

  public void dismissVignette() {
    if (!hasVignetteMarker()) return;
    for (WebElement frame : driver.findElements(By.cssSelector("iframe[title='Advertisement']"))) {
      try {
        driver.switchTo().frame(frame);
        if (closeInFrame(0)) return;
      } catch (StaleElementReferenceException | NoSuchFrameException changedFrame) {
        LOG.debug("Advertisement frame changed during navigation", changedFrame);
      } finally {
        driver.switchTo().defaultContent();
      }
    }
  }

  private boolean hasCloseControl(int depth) {
    for (WebElement close : driver.findElements(By.id("dismiss-button"))) {
      var targets = new ArrayList<WebElement>();
      targets.add(close);
      targets.addAll(close.findElements(By.cssSelector("*")));
      for (WebElement target : targets) {
        Rectangle bounds = target.getRect();
        if (target.isDisplayed()
            && target.isEnabled()
            && bounds.getWidth() > 0
            && bounds.getHeight() > 0) return true;
      }
    }
    if (depth >= 3) return false;
    for (WebElement child : driver.findElements(By.cssSelector("iframe"))) {
      try {
        driver.switchTo().frame(child);
        if (hasCloseControl(depth + 1)) return true;
      } finally {
        driver.switchTo().parentFrame();
      }
    }
    return false;
  }

  private boolean closeInFrame(int depth) {
    for (WebElement close : driver.findElements(By.id("dismiss-button"))) {
      // Some Google layouts use a zero-sized wrapper with a visible child Close control.
      var targets = new ArrayList<WebElement>();
      targets.add(close);
      targets.addAll(close.findElements(By.cssSelector("*")));
      for (WebElement target : targets) {
        Rectangle bounds = target.getRect();
        if (!target.isDisplayed()
            || !target.isEnabled()
            || bounds.getWidth() <= 0
            || bounds.getHeight() <= 0) continue;
        try {
          target.click();
          dismissalCount++;
          LOG.info(
              "Closed ordinary advertisement through its exposed control (frame depth {})", depth);
          return true;
        } catch (ElementNotInteractableException layoutChanging) {
          // The bounded explicit wait will inspect the next usable control or poll again.
          LOG.debug("Advertisement Close control is not ready for a native click", layoutChanging);
        }
      }
    }
    // Inspect descendants only after entering a known Advertisement frame.
    if (depth >= 3) return false;
    for (WebElement child : driver.findElements(By.cssSelector("iframe"))) {
      try {
        driver.switchTo().frame(child);
        if (closeInFrame(depth + 1)) return true;
      } finally {
        driver.switchTo().parentFrame();
      }
    }
    return false;
  }
}
