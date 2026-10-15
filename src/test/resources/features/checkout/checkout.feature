@regression @checkout
Feature: Checkout and order placement
  Background:
    Given the customer opens the store

  @negative
  Scenario: Checkout requires a customer account
    When the customer adds 1 of "Blue Top" to the cart
    And the guest tries to check out
    Then checkout requests registration or login

  @smoke @positive
  Scenario: Checkout reviews customer addresses and order totals
    Given the customer opens signup and login
    And a newly registered customer is signed in
    When the customer adds 2 of "Blue Top" to the cart
    And the signed in customer proceeds to checkout
    Then checkout shows the synthetic billing and delivery addresses
    And order review contains 2 of "Blue Top" with the correct total

  @negative
  Scenario: Payment requires cardholder information
    Given the customer opens signup and login
    And a newly registered customer is signed in
    When the customer adds 1 of "Blue Top" to the cart
    And the signed in customer proceeds to checkout
    And the customer opens payment
    And the customer submits empty payment details
    Then payment requires the cardholder name

  @positive
  Scenario: Place a synthetic demo order
    Given the customer opens signup and login
    And a newly registered customer is signed in
    When the customer adds 1 of "Blue Top" to the cart
    And the signed in customer proceeds to checkout
    And the customer opens payment
    And the customer places a synthetic demo order
    Then the order is successfully confirmed
