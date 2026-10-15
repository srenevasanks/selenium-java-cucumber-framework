package framework.utils;

import java.util.UUID;

public record Customer(String name, String email, String password) {
  public static Customer unique() {
    return new Customer(
        "Automation Customer",
        "portfolio." + UUID.randomUUID() + "@example.com",
        "Aa9!" + UUID.randomUUID());
  }

  @Override
  public String toString() {
    return "Synthetic Customer [credentials redacted]";
  }
}
