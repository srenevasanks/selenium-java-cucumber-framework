@regression @authentication
Feature: Customer authentication
  Background:
    Given the customer opens the store
    And the customer opens signup and login

  @smoke @positive
  Scenario: Customer logs in with new credentials and logs out
    Given a newly registered customer is signed in
    When the customer logs out
    And the customer signs in with the new account
    Then the correct customer identity is displayed
    When the customer logs out
    Then the customer is logged out

  @negative
  Scenario: Invalid credentials are rejected
    When the customer submits login email "nonexistent-portfolio@example.com" and password "SyntheticInvalid9!"
    Then invalid credentials are rejected

  @negative
  Scenario Outline: Login requires email and password
    When the customer submits login email "<email>" and password "<password>"
    Then login requires the "<field>" field
    Examples:
      | email            | password       | field    |
      |                  | SyntheticTest9 | email    |
      | test@example.com |                | password |
