# Implemented test coverage

Six feature files expand to **30 scenarios**. All scenarios are tagged `@regression`; **8** are tagged `@smoke`.

| Module | Expanded scenarios | Assertions |
|---|---:|---|
| Registration | 8 | Unique account confirmation and identity; missing name/email; malformed email; missing password, first name or address; duplicate email rejection |
| Authentication | 4 | Valid credential login, identity and logout; invalid credentials; missing email/password |
| Catalog | 6 | Search Blue Top and Men Tshirt; nonexistent search; price/category/brand/availability/condition on PDP; women/tops navigation; Polo filtering |
| Cart | 7 | Quantity 1 and 3; two different products and Rs. 1,400 subtotal; cumulative additions; removal/empty state; persistence after navigation; fresh empty session |
| Checkout | 4 | Guest authentication requirement; billing/delivery and total review; mandatory payment field; simulated order confirmation |
| End-to-end | 1 | Register → logout → credential login → search → PDP → quantity 2 → cart → checkout review → synthetic payment → confirmed order |

Feature files use Background, Scenario, Scenario Outline and Examples where useful. Steps are reusable and all are implemented.

## Meaningful validation

Product names and prices are checked against fixtures. Quantity and line totals use exact decimal arithmetic; the multiple-product cart also checks the number of rows and subtotal. Checkout checks all synthetic address fields, product identity/quantity and displayed order total. Confirmation checks both heading and confirmation message.

Negative required-field scenarios submit incomplete forms and inspect browser validity and the actual validation message. They do not assert only that an input exists. The invalid-email scenario checks email type mismatch. Authentication and duplicate registration check the application's real rejection messages.

## Supported scope

The public application's checkout is simulated. An all-zero card number is intentionally synthetic; success does not imply payment-provider validation. Address information is collected during signup and reviewed during checkout.

The inspected site does not expose wishlist, product sorting, password confirmation, inline cart quantity editing or selectable shipping/payment methods. No placeholder or intentionally skipped scenarios represent these unavailable features. Quantity changes are tested through the actual product quantity control and repeated additions.

## Tags

`@smoke`, `@regression`, `@positive`, `@negative`, `@registration`, `@authentication`, `@catalog`, `@cart`, `@checkout`, `@e2e`.

Scenarios are independent and create fresh browser sessions. Registered customer scenarios generate a unique account and attempt deletion in an after hook. No scenario depends on a previous scenario's account or cart.

Actual execution results are documented separately in [execution-summary.md](execution-summary.md).

## Ad-handler component coverage

Eight additional TestNG browser checks use deterministic fixtures for a direct advertising frame, a nested close control, a zero-sized wrapper with a visible child, an ad triggered by an in-page click and a retained URL marker after dismissal. Each asserts that the Close control actually dismisses the ad and that the page action can continue. `mvn clean test` therefore runs 30 live Cucumber scenarios plus 8 component checks. Cucumber tags filter the application scenarios; the eight component checks still execute unless `-Dtest=RunCucumberTest` is supplied.

Three further checks verify that a link consumed by an ad can continue after its real Close control is used, that an ad-consumed category expansion actually opens its panel, and that native clear/type operations work on separated fields when the page uses smooth scrolling and has a fixed bottom banner. These check specific synchronization failures seen during visible IDE execution.
