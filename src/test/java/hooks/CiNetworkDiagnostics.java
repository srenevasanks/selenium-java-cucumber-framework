package hooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.driver.DriverManager;
import io.cucumber.java.Scenario;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogType;

/** Opt-in CI evidence only: no request bodies, cookies, headers, query strings or interception. */
final class CiNetworkDiagnostics {
  private CiNetworkDiagnostics() {}

  static void capture(Scenario scenario) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("capturedUtc", java.time.Instant.now().toString());
    report.put("scenario", scenario.getName());
    report.put("line", scenario.getLine());
    report.put("statusBeforeCleanup", scenario.getStatus().name());
    List<Map<String, Object>> events = new ArrayList<>();
    Map<String, String> requests = new HashMap<>();
    try {
      for (var entry : DriverManager.get().manage().logs().get(LogType.PERFORMANCE)) {
        JsonNode message = mapper.readTree(entry.getMessage()).path("message");
        String method = message.path("method").asText();
        JsonNode params = message.path("params");
        String id = params.path("requestId").asText();
        if (method.equals("Network.requestWillBeSent")) {
          requests.put(id, safeUrl(params.path("request").path("url").asText()));
        }
        if (!Set.of(
                "Network.responseReceived",
                "Network.loadingFailed",
                "Network.loadingFinished",
                "Page.domContentEventFired",
                "Page.loadEventFired")
            .contains(method)) continue;
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", method);
        event.put("wallTimeMillis", entry.getTimestamp());
        event.put("frameId", params.path("frameId").asText());
        event.put("timestamp", params.path("timestamp").asDouble());
        if (!id.isEmpty()) {
          event.put("requestId", id);
          event.put("url", requests.getOrDefault(id, "unknown"));
        }
        if (method.equals("Network.responseReceived")) {
          JsonNode response = params.path("response");
          event.put("url", safeUrl(response.path("url").asText()));
          event.put("type", params.path("type").asText());
          event.put("status", response.path("status").asInt());
          event.put("mimeType", response.path("mimeType").asText());
          event.put("protocol", response.path("protocol").asText());
          event.put("requestTime", response.path("timing").path("requestTime").asDouble());
          event.put(
              "receiveHeadersEndMs", response.path("timing").path("receiveHeadersEnd").asDouble());
        } else if (method.equals("Network.loadingFailed")) {
          event.put("errorText", params.path("errorText").asText());
          event.put("type", params.path("type").asText());
          event.put("canceled", params.path("canceled").asBoolean());
          event.put("blockedReason", params.path("blockedReason").asText());
        } else if (method.equals("Page.frameNavigated")) {
          event.put("frameId", params.path("frame").path("id").asText());
          event.put("parentFrameId", params.path("frame").path("parentId").asText());
          event.put("url", safeUrl(params.path("frame").path("url").asText()));
        } else if (method.equals("Network.loadingFinished")) {
          event.put("encodedDataLength", params.path("encodedDataLength").asDouble());
        }
        events.add(event);
      }
    } catch (WebDriverException unavailable) {
      // Optional logging must preserve the original scenario result if a renderer/session is gone.
      report.put("diagnosticsUnavailable", unavailable.getClass().getSimpleName());
    }
    report.put("events", events);
    Path directory = Path.of("target/test-output/network");
    Files.createDirectories(directory);
    byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
    Files.write(directory.resolve(UUID.randomUUID() + ".json"), json);
    scenario.attach(json, "application/json", "Sanitized network diagnostics");
  }

  private static String safeUrl(String value) {
    try {
      URI uri = URI.create(value);
      if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())))
        return "non-http-resource";
      return uri.getScheme() + "://" + uri.getHost() + uri.getRawPath();
    } catch (IllegalArgumentException invalid) {
      return "unparseable-resource";
    }
  }
}
