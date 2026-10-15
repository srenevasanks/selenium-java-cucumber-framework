package framework.utils;

import java.math.BigDecimal;

public final class Money {
  private Money() {}

  public static BigDecimal parse(String value) {
    String number = value.replace("Rs.", "").replace(",", "").strip();
    if (!number.matches("\\d+(\\.\\d{1,2})?"))
      throw new IllegalArgumentException("Invalid displayed price: " + value);
    return new BigDecimal(number);
  }
}
