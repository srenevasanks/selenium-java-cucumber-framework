package stepdefinitions;

import framework.components.Header;
import framework.driver.DriverManager;
import framework.pages.*;
import framework.utils.*;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import java.math.BigDecimal;
import org.testng.Assert;

public final class StoreSteps {
  private Customer customer;
  private boolean registered;
  private String creationConfirmation;

  @Given("the customer opens the store")
  public void openStore() {
    new Header().open("");
  }

  @Given("the customer opens signup and login")
  public void openLogin() {
    new Header().login();
  }

  private void register() {
    customer = Customer.unique();
    RegistrationPage page = new RegistrationPage();
    page.start(customer);
    page.complete(customer.password(), "");
    creationConfirmation = page.awaitCreated();
    registered = true;
    page.continueToStore();
  }

  @When("the customer registers with unique valid details")
  public void registerValid() {
    register();
  }

  @Given("a newly registered customer is signed in")
  public void registeredCustomer() {
    register();
  }

  @Then("registration is confirmed and the customer is signed in")
  public void registrationConfirmed() {
    Assert.assertTrue(creationConfirmation.contains("ACCOUNT CREATED!"), creationConfirmation);
    signedIn();
  }

  @When("the customer starts signup with name {string} and email {string}")
  public void signup(String name, String email) {
    new RegistrationPage().signup(name, email);
  }

  @Then("signup requires the {string} field")
  public void signupMissing(String field) {
    RegistrationPage page = new RegistrationPage();
    String css = RegistrationPage.signupField(field);
    Assert.assertTrue(
        page.hasMissingValue(css), "Expected browser required-field validation: " + field);
    Assert.assertFalse(page.validationMessage(css).isBlank());
  }

  @Then("signup rejects the malformed email")
  public void invalidSignupEmail() {
    Assert.assertTrue(
        new RegistrationPage().hasEmailTypeMismatch(RegistrationPage.signupField("email")));
    Assert.assertFalse(
        new RegistrationPage().validationMessage(RegistrationPage.signupField("email")).isBlank());
  }

  @When("the customer submits registration without {string}")
  public void missingProfile(String omitted) {
    customer = Customer.unique();
    RegistrationPage page = new RegistrationPage();
    page.start(customer);
    page.complete(customer.password(), omitted);
  }

  @Then("registration requires {string}")
  public void profileRequired(String field) {
    RegistrationPage page = new RegistrationPage();
    Assert.assertTrue(page.hasMissingValue("#" + field), "Expected missing " + field);
    Assert.assertFalse(page.validationMessage("#" + field).isBlank());
  }

  @When("the customer attempts signup with the existing email")
  public void duplicateSignup() {
    new RegistrationPage().signup(customer.name(), customer.email());
  }

  @Then("duplicate signup is rejected")
  public void duplicateRejected() {
    Assert.assertEquals(new RegistrationPage().error(), "Email Address already exist!");
  }

  @When("the customer logs out")
  public void logout() {
    new Header().logout();
  }

  @When("the customer signs in with the new account")
  public void loginNew() {
    LoginPage page = new LoginPage();
    page.submit(customer.email(), customer.password());
    page.awaitLogin();
  }

  @Then("the correct customer identity is displayed")
  public void signedIn() {
    Header header = new Header();
    Assert.assertTrue(header.loggedIn());
    Assert.assertTrue(header.identity().contains("Logged in as " + customer.name()));
  }

  @Then("the customer is logged out")
  public void signedOut() {
    Assert.assertFalse(new Header().loggedIn());
    Assert.assertTrue(new LoginPage().displayed("[data-qa='login-button']"));
  }

  @When("the customer submits login email {string} and password {string}")
  public void submitLogin(String email, String password) {
    new LoginPage().submit(email, password);
  }

  @Then("invalid credentials are rejected")
  public void invalidCredentials() {
    Assert.assertEquals(new LoginPage().error(), "Your email or password is incorrect!");
    Assert.assertFalse(new Header().loggedIn());
  }

  @Then("login requires the {string} field")
  public void loginMissing(String field) {
    LoginPage page = new LoginPage();
    Assert.assertTrue(page.hasMissingValue(LoginPage.field(field)));
    Assert.assertFalse(page.validationMessage(LoginPage.field(field)).isBlank());
  }

  @When("the customer searches for {string}")
  public void search(String query) {
    new Header().products();
    new CatalogPage().search(query);
  }

  @Then("search results include {string}")
  public void foundProduct(String product) {
    CatalogPage page = new CatalogPage();
    Assert.assertEquals(page.heading(), "SEARCHED PRODUCTS");
    Assert.assertTrue(page.resultCount() > 0);
    Assert.assertTrue(page.results().contains(product));
  }

  @Then("search shows no matching products")
  public void noProducts() {
    Assert.assertEquals(new CatalogPage().heading(), "SEARCHED PRODUCTS");
    Assert.assertEquals(new CatalogPage().resultCount(), 0);
  }

  @When("the customer opens product {string} from the catalog")
  public void openProduct(String product) {
    new Header().products();
    new CatalogPage().viewProduct(product);
  }

  @When("the customer opens search result {string}")
  public void openResult(String product) {
    new CatalogPage().viewProduct(product);
  }

  @Then("product {string} shows its expected details")
  public void details(String product) {
    String details = new ProductPage().details();
    var fixture = TestData.product(product);
    for (String attribute :
        new String[] {
          product,
          "Rs. " + fixture.get("price").asInt(),
          fixture.get("category").asText(),
          fixture.get("brand").asText(),
          "In Stock",
          "New"
        }) {
      Assert.assertTrue(
          details.contains(attribute),
          "Missing displayed attribute: " + attribute + " in " + details);
    }
  }

  @When("the customer browses the women tops category")
  public void category() {
    new Header().products();
    new CatalogPage().womenTops();
  }

  @Then("the women tops category contains Blue Top")
  public void categoryResults() {
    Assert.assertEquals(new CatalogPage().heading().toUpperCase(), "WOMEN - TOPS PRODUCTS");
    Assert.assertTrue(new CatalogPage().results().contains("Blue Top"));
    new CatalogPage().viewProduct("Blue Top");
    Assert.assertTrue(new ProductPage().details().contains("Women > Tops"));
  }

  @When("the customer filters products by Polo")
  public void filterBrand() {
    new Header().products();
    new CatalogPage().polo();
  }

  @Then("the Polo results contain a product of that brand")
  public void brandResults() {
    Assert.assertTrue(new CatalogPage().heading().toUpperCase().contains("POLO"));
    Assert.assertTrue(new CatalogPage().results().contains("Blue Top"));
    new CatalogPage().viewProduct("Blue Top");
    Assert.assertTrue(new ProductPage().details().contains("Brand: Polo"));
  }

  @When("the customer adds {int} of the open product to the cart")
  public void addOpenProduct(int quantity) {
    new ProductPage().add(quantity);
    new ProductPage().viewCart();
  }

  @When("the customer adds {int} of {string} to the cart")
  public void addProduct(int quantity, String product) {
    openProduct(product);
    addOpenProduct(quantity);
  }

  @Then("the cart contains {int} of {string} with the correct price and line total")
  public void verifyLine(int quantity, String product) {
    CartPage page = new CartPage();
    BigDecimal unit = TestData.product(product).get("price").decimalValue();
    Assert.assertEquals(page.productName(product), product);
    Assert.assertEquals(page.quantity(product), quantity);
    Assert.assertEquals(page.unitPrice(product).compareTo(unit), 0);
    Assert.assertEquals(
        page.total(product).compareTo(unit.multiply(BigDecimal.valueOf(quantity))), 0);
  }

  @Then("the cart has {int} product lines and subtotal {int} rupees")
  public void verifySubtotal(int count, int subtotal) {
    Assert.assertEquals(new CartPage().size(), count);
    Assert.assertEquals(new CartPage().subtotal().compareTo(BigDecimal.valueOf(subtotal)), 0);
  }

  @When("the customer removes {string} from the cart")
  public void removeProduct(String product) {
    new CartPage().remove(product);
  }

  @When("the customer opens the cart")
  public void openCart() {
    new Header().cart();
  }

  @When("the customer returns home and revisits the cart")
  public void revisitCart() {
    new Header().home();
    new Header().cart();
  }

  @Then("the cart is empty")
  public void emptyCart() {
    Assert.assertEquals(new CartPage().size(), 0);
    Assert.assertTrue(new CartPage().emptyMessage().contains("Cart is empty!"));
  }

  @When("the guest tries to check out")
  public void guestCheckout() {
    new CartPage().guestCheckout();
  }

  @Then("checkout requests registration or login")
  public void guestRestriction() {
    Assert.assertTrue(
        new CartPage().guestMessage().contains("Register / Login account to proceed on checkout."));
  }

  @When("the signed in customer proceeds to checkout")
  public void checkout() {
    new CartPage().checkout();
  }

  @Then("checkout shows the synthetic billing and delivery addresses")
  public void addresses() {
    CheckoutPage page = new CheckoutPage();
    for (String field :
        new String[] {
          "first_name", "last_name", "address1", "state", "city", "zipcode", "mobile_number"
        }) {
      String expected = TestData.address().get(field).asText();
      Assert.assertTrue(page.delivery().contains(expected), "Delivery address missing " + field);
      Assert.assertTrue(page.billing().contains(expected), "Billing address missing " + field);
    }
  }

  @Then("order review contains {int} of {string} with the correct total")
  public void review(int quantity, String product) {
    Assert.assertTrue(new CheckoutPage().body().contains("Review Your Order"));
    verifyLine(quantity, product);
    BigDecimal expected =
        TestData.product(product)
            .get("price")
            .decimalValue()
            .multiply(BigDecimal.valueOf(quantity));
    Assert.assertEquals(new CheckoutPage().reviewTotal().compareTo(expected), 0);
  }

  @When("the customer opens payment")
  public void payment() {
    new CheckoutPage().payment();
  }

  @When("the customer submits empty payment details")
  public void emptyPayment() {
    new CheckoutPage().submitEmptyPayment();
  }

  @Then("payment requires the cardholder name")
  public void paymentRequired() {
    CheckoutPage page = new CheckoutPage();
    Assert.assertTrue(page.hasMissingValue("[data-qa='name-on-card']"));
    Assert.assertFalse(page.validationMessage("[data-qa='name-on-card']").isBlank());
    Assert.assertTrue(page.displayed("[data-qa='pay-button']"));
  }

  @When("the customer places a synthetic demo order")
  public void placeOrder() {
    new CheckoutPage().placeSyntheticOrder();
  }

  @Then("the order is successfully confirmed")
  public void orderConfirmed() {
    Assert.assertEquals(new CheckoutPage().confirmation().toUpperCase(), "ORDER PLACED!");
    Assert.assertTrue(
        new CheckoutPage().body().contains("Congratulations! Your order has been confirmed!"));
  }

  @After(order = 100)
  public void deleteDisposableAccount() {
    if (!registered || !DriverManager.exists()) return;
    Header header = new Header();
    if (!header.loggedIn()) {
      header.login();
      loginNew();
    }
    header.deleteTestAccount();
    registered = false;
  }
}
