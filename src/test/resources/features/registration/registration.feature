@regression @registration
Feature: Customer registration
  Background:
    Given the customer opens the store
    And the customer opens signup and login

  @smoke @positive
  Scenario: Register a unique customer
    When the customer registers with unique valid details
    Then registration is confirmed and the customer is signed in

  @negative
  Scenario Outline: Signup requires basic account details
    When the customer starts signup with name "<name>" and email "<email>"
    Then signup requires the "<field>" field
    Examples:
      | name       | email            | field |
      |            | test@example.com | name  |
      | Automation |                  | email |

  @negative
  Scenario: Signup rejects malformed email
    When the customer starts signup with name "Automation" and email "invalid-email"
    Then signup rejects the malformed email

  @negative
  Scenario Outline: Registration requires profile and address information
    When the customer submits registration without "<field>"
    Then registration requires "<field>"
    Examples:
      | field      |
      | password   |
      | first_name |
      | address1   |

  @negative
  Scenario: Existing email cannot be registered twice
    Given a newly registered customer is signed in
    When the customer logs out
    And the customer attempts signup with the existing email
    Then duplicate signup is rejected
