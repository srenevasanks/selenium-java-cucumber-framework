package framework.components;

import framework.pages.BasePage;

public final class Header extends BasePage {
  public void login() {
    transition("header a[href='/login']", "/login", "[data-qa='login-email']");
  }

  public void products() {
    transition("header a[href='/products']", "/products", "#search_product");
  }

  public void cart() {
    transition("header a[href='/view_cart']", "/view_cart", "#cart_items");
  }

  public void home() {
    transition("header a[href='/']", "/", "#slider-carousel");
  }

  public void logout() {
    transition("header a[href='/logout']", "/login", "[data-qa='login-email']");
  }

  public boolean loggedIn() {
    return displayed("header a[href='/logout']");
  }

  public String identity() {
    return text("header");
  }

  public void deleteTestAccount() {
    transition(
        "header a[href='/delete_account']", "/delete_account", "[data-qa='continue-button']");
    if (!body().contains("ACCOUNT DELETED!"))
      throw new IllegalStateException("Account deletion not confirmed");
  }
}
