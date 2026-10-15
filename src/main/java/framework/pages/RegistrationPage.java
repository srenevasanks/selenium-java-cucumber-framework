package framework.pages;

import framework.utils.Customer;
import framework.utils.TestData;

public final class RegistrationPage extends BasePage {
  public static String signupField(String name) {
    return "[data-qa='signup-" + name + "']";
  }

  public void signup(String name, String email) {
    fill(signupField("name"), name);
    fill(signupField("email"), email);
    click("[data-qa='signup-button']");
  }

  public void start(Customer customer) {
    signup(customer.name(), customer.email());
    awaitPage("/signup", "#first_name");
  }

  public void complete(String password, String omitted) {
    fill("#password", password);
    for (String field :
        new String[] {
          "first_name", "last_name", "address1", "state", "city", "zipcode", "mobile_number"
        }) {
      if (!field.equals(omitted)) fill("#" + field, TestData.address().get(field).asText());
    }
    if ("password".equals(omitted)) fill("#password", "");
    click("[data-qa='create-account']");
  }

  public String awaitCreated() {
    awaitPage("/account_created", "[data-qa='continue-button']");
    return body();
  }

  public void continueToStore() {
    transition("[data-qa='continue-button']", "/", "header a[href='/logout']");
  }

  public String error() {
    return text(".signup-form form p");
  }
}
