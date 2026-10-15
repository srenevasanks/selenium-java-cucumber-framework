package framework.pages;

import framework.utils.Money;
import framework.utils.TestData;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

public final class CartPage extends BasePage {
  private String row(String product) {
    return "#product-" + TestData.product(product).get("id").asInt();
  }

  public int quantity(String product) {
    return Integer.parseInt(text(row(product) + " .cart_quantity button"));
  }

  public String productName(String product) {
    return text(row(product) + " .cart_description h4");
  }

  public BigDecimal unitPrice(String product) {
    return Money.parse(text(row(product) + " .cart_price"));
  }

  public BigDecimal total(String product) {
    return Money.parse(text(row(product) + " .cart_total_price"));
  }

  public int size() {
    return driver.findElements(By.cssSelector("#cart_info_table tbody tr[id^='product-']")).size();
  }

  public BigDecimal subtotal() {
    return driver.findElements(By.cssSelector("#cart_info_table tbody .cart_total_price")).stream()
        .map(e -> Money.parse(e.getText()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void remove(String product) {
    click(row(product) + " .cart_quantity_delete");
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(row(product))));
  }

  public String emptyMessage() {
    return text("#empty_cart");
  }

  public void checkout() {
    transition("a.check_out", "/checkout", "#address_delivery");
  }

  public void guestCheckout() {
    click("a.check_out");
    visible("#checkoutModal");
  }

  public String guestMessage() {
    return text("#checkoutModal");
  }
}
