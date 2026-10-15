package components;

import com.sun.net.httpserver.HttpServer;
import framework.driver.DriverManager;
import framework.pages.BasePage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Deterministic browser regression fixtures for an otherwise nondeterministic third-party ad. */
public final class AdvertisementTest {
  private HttpServer server;
  private volatile byte[] fixture = new byte[0];

  @BeforeMethod
  public void startBrowser() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "text/html;charset=utf-8");
          exchange.sendResponseHeaders(200, fixture.length);
          try (var body = exchange.getResponseBody()) {
            body.write(fixture);
          } finally {
            exchange.close();
          }
        });
    server.start();
    DriverManager.start();
  }

  @AfterMethod(alwaysRun = true)
  public void stopBrowser() {
    try {
      DriverManager.stop();
    } finally {
      if (server != null) server.stop(0);
    }
  }

  @Test
  public void closesDirectAdBeforeClickingThePage() {
    openFixture(false, false);
    new FixturePage().continueJourney();
    assertClosedAndPageUsable();
  }

  @Test
  public void closesNestedAdAndRestoresThePageContext() {
    openFixture(true, false);
    new FixturePage().continueJourney();
    assertClosedAndPageUsable();
  }

  @Test
  public void closesAdTriggeredByAnInPageInteraction() {
    openFixture(false, true);
    FixturePage page = new FixturePage();
    page.showAd();
    Assert.assertEquals(DriverManager.get().findElement(By.id("ad-status")).getText(), "closed");
    page.continueJourney();
    assertClosedAndPageUsable();
  }

  @Test
  public void clicksVisibleCloseChildInsideZeroSizedWrapper() {
    openFixture(false, false, true);
    new FixturePage().continueJourney();
    assertClosedAndPageUsable();
  }

  @Test
  public void resumesWhenClosedAdLeavesItsUrlMarkerBehind() {
    openFixture(false, false, false, true);
    new FixturePage().continueJourney();
    assertClosedAndPageUsable(true);
  }

  @Test
  public void resumesLinkConsumedByAnAdvertisement() {
    loadFixture("""
        <!doctype html><html><body>
        <a id="navigate" href="/destination" onclick="
          if (!window.adWasClosed) {
            event.preventDefault();
            document.getElementById('advert').style.display='block';
            location.hash='google_vignette';
          }">Continue journey</a>
        <p id="destination">Destination content</p>
        <iframe id="advert" title="Advertisement" style="display:none;position:fixed;inset:0;width:90vw;height:90vh"
          srcdoc="&lt;button id='dismiss-button' onclick='parent.closeAd()'&gt;Close&lt;/button&gt;"></iframe>
        <script>function closeAd() {
          window.adWasClosed=true;
          document.getElementById('advert').remove();
          location.hash='';
        }</script></body></html>
        """, "");
    new FixturePage().navigate();
    Assert.assertTrue(DriverManager.get().getCurrentUrl().endsWith("/destination"));
  }

  @Test
  public void fillsOffscreenFieldsDespiteSmoothScrollingAndBottomBanner() {
    loadFixture("""
        <!doctype html><html style="scroll-behavior:smooth"><body>
        <input id="first" value="previous">
        <div style="height:1600px"></div>
        <input id="last" value="previous"><div style="height:600px"></div>
        <aside style="position:fixed;bottom:0;left:0;right:0;height:150px;background:gray">Banner</aside>
        </body></html>
        """, "");
    new FixturePage().fillSeparatedFields();
    Assert.assertEquals(DriverManager.get().findElement(By.id("first")).getDomProperty("value"), "first value");
    Assert.assertEquals(DriverManager.get().findElement(By.id("last")).getDomProperty("value"), "last value");
  }

  @Test
  public void expandsCategoryWhenAdvertisementConsumedTheFirstClick() {
    loadFixture("""
        <!doctype html><html><body>
        <a id="expand" href="#panel" onclick="event.preventDefault();
          if (window.adWasClosed) document.getElementById('panel').style.display='block';
          else {document.getElementById('advert').style.display='block';location.hash='google_vignette';}">Category</a>
        <div id="panel" style="display:none">Category products</div>
        <iframe id="advert" title="Advertisement" style="display:none;position:fixed;inset:0;width:90vw;height:90vh"
          srcdoc="&lt;button id='dismiss-button' onclick='parent.closeAd()'&gt;Close&lt;/button&gt;"></iframe>
        <script>function closeAd() {
          window.adWasClosed=true;
          document.getElementById('advert').remove();
          location.hash='';
        }</script></body></html>
        """, "");
    new FixturePage().expandCategory();
    Assert.assertTrue(DriverManager.get().findElement(By.id("panel")).isDisplayed());
  }

  private void assertClosedAndPageUsable() {
    assertClosedAndPageUsable(false);
  }

  private void assertClosedAndPageUsable(boolean markerRetained) {
    Assert.assertEquals(DriverManager.get().findElement(By.id("status")).getText(), "continued");
    Assert.assertEquals(DriverManager.get().findElement(By.id("ad-status")).getText(), "closed");
    Assert.assertEquals(
        DriverManager.get().getCurrentUrl().contains("#google_vignette"), markerRetained);
    Assert.assertTrue(DriverManager.get().findElements(By.id("advert")).isEmpty());
  }

  private void openFixture(boolean nested, boolean late) {
    openFixture(nested, late, false);
  }

  private void openFixture(boolean nested, boolean late, boolean zeroSizedWrapper) {
    openFixture(nested, late, zeroSizedWrapper, false);
  }

  private void openFixture(
      boolean nested, boolean late, boolean zeroSizedWrapper, boolean keepMarker) {
    String closeTarget = nested ? "parent.parent" : "parent";
    String content =
        "<button id='dismiss-button' onclick=\"" + closeTarget + ".closeAd()\">Close ad</button>";
    if (zeroSizedWrapper) {
      content =
          "<div id='dismiss-button' style='width:0;height:0;overflow:visible' onclick=\""
              + closeTarget
              + ".closeAd()\"><button>Close ad</button></div>";
    }
    if (nested) content = "<iframe srcdoc=\"" + attribute(content) + "\"></iframe>";
    String html =
        """
<!doctype html><html><head><title>Ad component regression fixture</title></head><body>
<h1>Ad component regression fixture</h1>
<p id="status">waiting</p><p id="ad-status">open</p>
<button id="continue" onclick="document.getElementById('status').textContent='continued'">Continue</button>
<button id="show" onclick="document.getElementById('advert').style.display='block';location.hash='google_vignette'">Show ad</button>
<script>
function closeAd() {
  document.getElementById('advert').remove();
  document.getElementById('ad-status').textContent='closed';
  location.hash='';
}
</script>
"""
            + "<iframe id='advert' title='Advertisement' style='position:fixed;top:0;left:0;"
            + "width:500px;height:250px;background:white;display:"
            + (late ? "none" : "block")
            + "' srcdoc=\""
            + attribute(content)
            + "\"></iframe></body></html>";
    if (keepMarker) html = html.replace("location.hash='';", "/* retain the navigation marker */");
    loadFixture(html, late ? "" : "#google_vignette");
  }

  private void loadFixture(String html, String fragment) {
    fixture = html.getBytes(StandardCharsets.UTF_8);
    DriverManager.get()
        .get(
            "http://127.0.0.1:"
                + server.getAddress().getPort()
                + "/fixture.html"
                + fragment);
  }

  private static String attribute(String html) {
    return html.replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private static final class FixturePage extends BasePage {
    void expandCategory() {
      expand("#expand", "#panel");
    }
    void navigate() {
      transition("#navigate", "/destination", "#destination");
    }

    void fillSeparatedFields() {
      fill("#first", "first value");
      fill("#last", "last value");
      fill("#first", "first value");
    }

    void continueJourney() {
      click("#continue");
    }

    void showAd() {
      click("#show");
    }
  }
}
