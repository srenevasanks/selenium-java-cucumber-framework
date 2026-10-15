@regression @catalog
Feature: Product discovery
  Background:
    Given the customer opens the store

  @smoke @positive
  Scenario Outline: Search for an existing product
    When the customer searches for "<product>"
    Then search results include "<product>"
    Examples:
      | product    |
      | Blue Top   |
      | Men Tshirt |

  @negative
  Scenario: Search for a nonexistent product
    When the customer searches for "portfolio-nonexistent-9173"
    Then search shows no matching products

  @positive
  Scenario: Product details display catalog attributes
    When the customer opens product "Blue Top" from the catalog
    Then product "Blue Top" shows its expected details

  @positive
  Scenario: Navigate through a category and subcategory
    When the customer browses the women tops category
    Then the women tops category contains Blue Top

  @positive
  Scenario: Filter catalog by brand
    When the customer filters products by Polo
    Then the Polo results contain a product of that brand
