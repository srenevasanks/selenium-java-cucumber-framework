package framework.pages;

public final class LoginPage extends BasePage {
  public static String field(String name) {
    return "[data-qa='login-" + name + "']";
  }

  public void submit(String email, String password) {
    fill(field("email"), email);
    fill(field("password"), password);
    click("[data-qa='login-button']");
  }

  public void awaitLogin() {
    awaitPage("/", "header a[href='/logout']");
  }

  public String error() {
    return text(".login-form form p");
  }
}
