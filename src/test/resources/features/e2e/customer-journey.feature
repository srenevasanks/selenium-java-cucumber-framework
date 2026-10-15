@regression @smoke @e2e @positive
Feature: Complete customer journey
  Scenario: Register login discover and purchase a product
    Given the customer opens the store
    And the customer opens signup and login
    When the customer registers with unique valid details
    Then registration is confirmed and the customer is signed in
    When the customer logs out
    And the customer signs in with the new account
    And the customer searches for "Blue Top"
    Then search results include "Blue Top"
    When the customer opens search result "Blue Top"
    Then product "Blue Top" shows its expected details
    When the customer adds 2 of the open product to the cart
    Then the cart contains 2 of "Blue Top" with the correct price and line total
    When the signed in customer proceeds to checkout
    Then checkout shows the synthetic billing and delivery addresses
    And order review contains 2 of "Blue Top" with the correct total
    When the customer opens payment
    And the customer places a synthetic demo order
    Then the order is successfully confirmed
