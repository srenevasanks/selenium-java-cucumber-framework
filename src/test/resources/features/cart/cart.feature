@regression @cart
Feature: Shopping cart
  Background:
    Given the customer opens the store

  @smoke @positive
  Scenario Outline: Add the selected quantity and validate monetary totals
    When the customer adds <quantity> of "Blue Top" to the cart
    Then the cart contains <quantity> of "Blue Top" with the correct price and line total
    Examples:
      | quantity |
      | 1        |
      | 3        |

  @positive
  Scenario: Cart contains multiple products
    When the customer adds 2 of "Blue Top" to the cart
    And the customer adds 1 of "Men Tshirt" to the cart
    Then the cart contains 2 of "Blue Top" with the correct price and line total
    And the cart contains 1 of "Men Tshirt" with the correct price and line total
    And the cart has 2 product lines and subtotal 1400 rupees

  @positive
  Scenario: Adding the same product increases its quantity
    When the customer adds 1 of "Blue Top" to the cart
    And the customer adds 2 of "Blue Top" to the cart
    Then the cart contains 3 of "Blue Top" with the correct price and line total

  @positive
  Scenario: Remove the last product from the cart
    When the customer adds 1 of "Blue Top" to the cart
    And the customer removes "Blue Top" from the cart
    Then the cart is empty

  @positive
  Scenario: Cart survives navigation
    When the customer adds 2 of "Blue Top" to the cart
    And the customer returns home and revisits the cart
    Then the cart contains 2 of "Blue Top" with the correct price and line total

  @negative
  Scenario: New browser session has an empty cart
    When the customer opens the cart
    Then the cart is empty
