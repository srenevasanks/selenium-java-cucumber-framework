package framework.pages;

import framework.utils.TestData;
import org.openqa.selenium.By;

public final class CatalogPage extends BasePage {
  public void search(String query) {
    fill("#search_product", query);
    click("#submit_search");
    wait.until(
        browser -> {
          return pageIsClear()
              && browser.getCurrentUrl().contains("search=")
              && browser
                  .findElement(By.cssSelector(".features_items h2"))
                  .getText()
                  .equals("SEARCHED PRODUCTS");
        });
  }

  public int resultCount() {
    return driver.findElements(By.cssSelector(".features_items .product-image-wrapper")).size();
  }

  public String results() {
    return text(".features_items");
  }

  public String heading() {
    return text(".features_items h2");
  }

  public void viewProduct(String name) {
    String path = "/product_details/" + TestData.product(name).get("id").asInt();
    transition("a[href='" + path + "']", path, ".product-information");
  }

  public void womenTops() {
    expand("a[href='#Women']", "#Women a[href='/category_products/2']");
    transition(
        "#Women a[href='/category_products/2']", "/category_products/2", ".features_items h2");
  }

  public void polo() {
    transition("a[href='/brand_products/Polo']", "/brand_products/Polo", ".features_items h2");
  }
}
