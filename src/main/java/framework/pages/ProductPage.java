package framework.pages;

public final class ProductPage extends BasePage {
  public String details() {
    return text(".product-information");
  }

  public void add(int quantity) {
    if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
    fill("#quantity", Integer.toString(quantity));
    click("button.cart");
    visible("#cartModal");
  }

  public void continueShopping() {
    click("#cartModal .close-modal");
    wait.until(
        org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(
            org.openqa.selenium.By.id("cartModal")));
  }

  public void viewCart() {
    transition("#cartModal a[href='/view_cart']", "/view_cart", "#cart_info_table");
  }
}
