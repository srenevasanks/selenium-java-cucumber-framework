package framework.pages;

import framework.utils.Money;
import java.math.BigDecimal;

public final class CheckoutPage extends BasePage {
  public String delivery() {
    return text("#address_delivery");
  }

  public String billing() {
    return text("#address_invoice");
  }

  public BigDecimal reviewTotal() {
    return Money.parse(text("#cart_info tbody tr:last-child .cart_total_price"));
  }

  public void payment() {
    fill("textarea[name='message']", "Synthetic portfolio automation order");
    transition("a[href='/payment']", "/payment", "[data-qa='name-on-card']");
  }

  public void submitEmptyPayment() {
    click("[data-qa='pay-button']");
  }

  public void placeSyntheticOrder() {
    fill("[data-qa='name-on-card']", "Synthetic Demo Only");
    fill("[data-qa='card-number']", "0000000000000000");
    fill("[data-qa='cvc']", "000");
    fill("[data-qa='expiry-month']", "12");
    fill("[data-qa='expiry-year']", "2030");
    click("[data-qa='pay-button']");
    wait.until(
        browser -> {
          return pageIsClear()
              && browser.getCurrentUrl().contains("/payment_done/")
              && displayed("[data-qa='order-placed']");
        });
  }

  public String confirmation() {
    return text("[data-qa='order-placed']");
  }
}
