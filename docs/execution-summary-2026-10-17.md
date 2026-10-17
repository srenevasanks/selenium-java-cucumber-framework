# Verified execution — 17 October 2026

Application: https://automationexercise.com/. The existing framework was reviewed and improved in place. Its feature files, step assertions, page/component behavior and eight component checks were preserved.

## Current local results

| Local execution | Live passed/total | Components passed/total | Total passed/total | Failed | Skipped | Surefire duration |
| --------------- | ----------------: | ----------------------: | -----------------: | -----: | ------: | ----------------: |
| Smoke           |               8/8 |                     8/8 |              16/16 |      0 |       0 |            75.9 s |
| Full regression |             30/30 |                     8/8 |              38/38 |      0 |       0 |           191.9 s |
| Components only |               0/0 |                     8/8 |                8/8 |      0 |       0 |            13.5 s |

All local executions used Java 17.0.12, Maven 3.9.11, Windows 11 and headless Chrome 155.0.8059.82. Full and smoke runs configured two Cucumber workers and both observed a peak of 2. The component-only class is sequential; its configured worker property is not a claim of two simultaneous component browsers.

Maven compilation succeeded. The full command ended BUILD SUCCESS in 3m 20s; the final smoke command ended BUILD SUCCESS in 1m 23s. Surefire durations above exclude Maven compilation/setup. No retry, skipped-case annotation or assertion change was used to obtain these results. An earlier smoke repetition also passed and is retained in [smoke-initial.json](review-2026-10-17/smoke-initial.json).

## Count consistency

Extent and Cucumber report live scenarios only: 30 in full regression, 8 in smoke. Surefire covers those plus the separate component checks. The automatic consolidated summary groups actual TestNG result sets and reports configuration failures/skips separately. It does not insert component results into Extent or hardcode suite totals.

[Regression](review-2026-10-17/regression.json), [smoke](review-2026-10-17/smoke.json) and [component-only](review-2026-10-17/components.json) summaries were cross-checked against actual TestNG/Surefire XML and Cucumber JSON. Cucumber event counts agree with the corresponding TestNG results. After the component-only invocation, stale Cucumber/Extent files were absent and the consolidated summary correctly recorded zero live cases.

## GitHub Actions

[Completed GitHub-hosted run](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39052648633): **failure** — 27/30 live scenarios, 8/8 component tests; 3 failed test methods and 0 skipped.

The workflow uses Ubuntu 24.04, Java 17, Maven dependency caching, stable Chrome with matching ChromeDriver, headless mode and two scenario workers. Its Maven command executes both existing discovered classes and preserves the real exit code. Category validation and a generated summary table run after failures; artifacts upload with always(). The archived run metadata includes the actual job-step conclusions and event counts. No application or GitHub secrets are stored in the repository or generated summary.

The final documentation-only update uses `[skip ci]` to avoid repeating a full regression against the public demo. The framework and workflow source are unchanged from the linked, completed validation run. Its failed status remains visible.

The previously completed workflow failed before application interaction. A Chrome-only probe identified Ubuntu user-namespace restrictions, then a root-owned Chrome sandbox helper with mode 777 instead of its required 4755. The workflow sets CHROME_DEVEL_SANDBOX to the packaged helper, verifies its root ownership, and restores the required permissions. It does not pass no-sandbox or globally disable AppArmor. This follows [Chromium's documented sandbox-helper approach](https://chromium.googlesource.com/chromium/src/+/main/docs/security/apparmor-userns-restrictions.md). The earlier failed run remains documented in [ci-startup-failure.json](review-2026-10-17/ci-startup-failure.json); a passing local run is not substituted for hosted evidence.

## Remaining hosted failures

The completed hosted run retained three live failures with no skipped scenarios:

- Login required-password example: [the screenshot](review-2026-10-17/ci-login-origin-tls-525.png) shows **SSL handshake failed, error 525**, an origin-service error rather than the expected homepage.
- Cart with multiple products: [the screenshot](review-2026-10-17/ci-cart-renderer-timeout.png) and exception show a 60-second Chrome renderer timeout during product navigation.
- Required registration profile/address example: [the screenshot](review-2026-10-17/ci-registration-renderer-timeout.png) and exception show a renderer timeout while opening the store.

The latter two are hosted timing/rendering failures; their exact underlying cause is not proven by the available artifacts. All three scenarios passed locally. I retained their failures and original expectations, without raising timeouts, adding retries, blocking advertising or changing application checks to obtain a green workflow. The summary-validation and artifact-upload steps succeeded despite the failed Maven command. This is a verified hosted execution, **not a claim of green CI**.

## Reports and preserved evidence

New runtime reports are generated at:

- target/test-output/extent-report.html — Cucumber scenarios only.
- target/test-output/cucumber.html — the same selected live scenarios.
- target/test-output/cucumber-summary.json — Cucumber event totals and observed peak concurrency.
- target/test-output/execution-summary.json — separate categories and aggregate TestNG test-method counts.
- target/surefire-reports/index.html — all tests, including components.

These ignored directories reflect the most recent command; the final local command was smoke. The full and smoke reports were archived separately before later commands. The full-run Extent, Cucumber and Surefire HTML reports were opened in real Chromium and showed 30 live passes, 100% Cucumber pass status and 38 passed methods respectively.

The existing [published Extent](https://srenevasanks.github.io/selenium-java-cucumber-framework/extent-report.html) and [published Cucumber](https://srenevasanks.github.io/selenium-java-cucumber-framework/cucumber.html) URLs remain functional and intentionally serve the **15 October 2026** snapshot. They were clicked from the GitHub README and verified in Chromium. They are not relabeled as this new review's reports.

All 31 previously tracked report files remain unchanged. [Preservation hashes](review-2026-10-17/preservation-check.json), [the original execution narrative](execution-summary-2026-10-15.md), reports/repeatability/ and docs/latest-execution.json retain historical evidence. [SNAPSHOT.md](../reports/SNAPSHOT.md) explains the frozen snapshot. Runtime logs, driver logs and temporary screenshots are ignored; reviewed evidence is added deliberately under a dated documentation directory.

## Commands actually executed

```bash
mvn --batch-mode --no-transfer-progress test-compile
mvn --batch-mode --no-transfer-progress clean test -Dcucumber.filter.tags=@smoke -Dheadless=true -Dscenarios.parallel=true -Dthreads=2
mvn --batch-mode --no-transfer-progress clean test -Dheadless=true -Dscenarios.parallel=true -Dthreads=2
mvn --batch-mode --no-transfer-progress test -Dtest=AdvertisementTest -Dheadless=true
```

The non-clean component-only command specifically checked stale Cucumber-report prevention. Use clean test for an entirely fresh Surefire directory: ordinary Maven test may retain XML from previously executed classes. TestNG's current result sets and the matching class XML were used for that independent component check.

## Remaining limits

Three hosted live failures remain documented above. Only Chrome is supported, as before. The public site's availability, advertising and fixture product data can change. Selenium's optional missing-CDP warning does not affect these W3C WebDriver tests. An abrupt JVM/runner termination can prevent final reports and account cleanup. No changes to application behavior, business assertions, test-data expectations or scenario coverage were made. See the [targeted review](review-2026-10-17/framework-review.md) and [updated architecture](framework-architecture.md).
