package framework.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public final class TestData {
  private static final JsonNode DATA;

  static {
    try (var stream = TestData.class.getClassLoader().getResourceAsStream("testdata/store.json")) {
      if (stream == null) throw new IllegalStateException("Missing testdata/store.json");
      DATA = new ObjectMapper().readTree(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read test data", e);
    }
  }

  private TestData() {}

  public static JsonNode address() {
    return DATA.get("address");
  }

  public static JsonNode product(String name) {
    for (JsonNode product : DATA.get("products"))
      if (product.get("name").asText().equals(name)) return product;
    throw new IllegalArgumentException("Unknown fixture product: " + name);
  }
}
