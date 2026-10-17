# GitHub Actions failure investigation â€” 18 October 2026 (IST)

I investigated only the three failures from [the original hosted regression](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39052648633). That run completed 38 test methods: 35 passed, three failed, zero skipped. The 30 live Cucumber scenarios and eight component checks are preserved. Existing business assertions, locators, feature examples, waits, data factories, page objects and component tests were not changed.

## Exact original failures

| Scenario / example                                                         | Source                    | Failing step and command                                                                                            | Exact error                                                                                                                                                                     | Screenshot                                                                          |
| -------------------------------------------------------------------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| Login requires email and password â€” missing password                     | authentication.feature:28 | Background: Given the customer opens the store; BasePage.awaitPage after navigation                                 | Expected condition failed: Expected page / with header a[href='/products']; actual URL: https://automationexercise.com/ (tried for 30 second(s) with 500 milliseconds interval) | [Origin TLS error 525](../review-2026-10-17/ci-login-origin-tls-525.png)            |
| Cart contains multiple products                                            | cart.feature:16           | When the customer adds 2 of "Blue Top" to the cart; Header.products â†’ BasePage.transition â†’ native clickElement | timeout: Timed out receiving message from renderer: 60.000                                                                                                                      | [Products page after timeout](../review-2026-10-17/ci-cart-renderer-timeout.png)    |
| Registration requires profile and address information â€” missing address1 | registration.feature:34   | Background: Given the customer opens the store; native get https://automationexercise.com                           | timeout: Timed out receiving message from renderer: 30.282                                                                                                                      | [Homepage after timeout](../review-2026-10-17/ci-registration-renderer-timeout.png) |

All three exceptions were Selenium TimeoutException. Steps after the failure were naturally not executed; the scenarios counted as failed, not skipped. The negative login/address assertions were not reached in their failed executions. [Exact scenario details](baseline-three-failures.json) include the resolved example steps and framework stack locations. [Screenshot hashes](baseline-screenshot-verification.json) verify the three images against their original Cucumber attachments.

## SSL 525 finding

The first screenshot explicitly shows **SSL handshake failed â€” error code 525**, with Browser Working, Cloudflare Working and automationexercise.com Host Error, timestamped **17 October 2026 17:12:45 UTC**. The Selenium failure followed at 17:13:15 UTC when the expected homepage header did not appear. This is evidence of an error page replacing the application before login, not incorrect password-field validation.

[Cloudflare defines 525](https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-525/) as a TLS handshake failure between its edge and the origin server. The old run did not collect original response headers or a HAR, so its HTTP status/CF-Ray cannot be independently reconstructed beyond the screenshot. The precise origin certificate, SNI, cipher, connection-reset or hosting cause needs the application's origin/Cloudflare logs. The public automation repository cannot repair that hop.

No certificate verification, Cloudflare challenge, browser identity or security control was changed. New browser diagnostics observed successful first-party document responses in the completed healthy runs. Separate ordinary Python HTTPS observations returned **403**, while normal Selenium browser traffic succeeded. Those non-browser requests were recorded unchanged, without retries, custom identities, shared browser cookies or challenge handling; they are not presented as an application API pass or as a reproduction of the old 525.

The final repeat also recorded one **HTTP 500 from the third-party advertising frame** `https://googleads.g.doubleclick.net/pagead/ads`. First-party application document observations remained 200 and all business assertions passed. This response is retained in the actual network status totals; it is not labelled an application failure or hidden.

## Renderer findings

The two errors came from **native navigation commands**: a click opening Products and get opening the homepage. They were not failed price/quantity/required-field comparisons or a missing-element exception. The 38 old ChromeDriver logs contain the matching renderer-timeout messages, with no recorded crash/disconnection/OOM signature. That is not a substitute for kernel logs: the old failing run has no CPU/RAM/network timeline, so an exact historical cause cannot be proved.

The screenshots show usable page content after the exceptions; they do not establish what the renderer or its subframes were doing during the stalled command. New performance observations include document status, resource completion/failure, frame navigation, DOM-content and load events. Page Load Strategy remains NORMAL and configured explicit/page-load waits remain **30/60 seconds**, respectively. No wait was raised or assertion relaxed.

The original examples passed in a targeted two-worker run and in the complete controlled runs below. The full two-worker run also passed all 38. Therefore I found **no reproducible business-assertion, locator, session-isolation or deterministic two-worker framework defect**. Transient application/network/resource loading or renderer delay remains possible; the exact old renderer cause is unresolved. Changing the page-load strategy, blocking advertising or adding retries would be an unproven workaround, so none was added.

## Local versus hosted configuration

| Setting                      | Local reference                 | Controlled GitHub runs                                   |
| ---------------------------- | ------------------------------- | -------------------------------------------------------- |
| OS                           | Windows 11 Pro                  | Ubuntu 24.04                                             |
| Java                         | Oracle 17.0.12                  | Temurin 17.0.20.1                                        |
| Maven                        | 3.9.11                          | 3.10.0                                                   |
| Selenium / Cucumber / TestNG | 4.35.0 / 7.27.2 / 7.11.0        | Same locked POM versions                                 |
| Chrome                       | 155.0.8059.82                   | 155.0.8059.84, pinned to the original failed run's build |
| ChromeDriver                 | 155.0.8059.84                   | 155.0.8059.84, matching the CI browser                   |
| Headless / window            | --headless=new / 1440Ã—1000     | Same                                                     |
| Page load / explicit wait    | NORMAL / 60 s / 30 s            | Same                                                     |
| Capacity measured            | 12 logical CPUs / 31.75 GiB RAM | 4 logical CPUs / 15.61 GiB RAM                           |
| Scenario workers             | Default remains 2               | Compared 2 and 1; CI default is 1                        |

The local Chrome and driver differ only at patch level within the same 155.0.8059 build; no major-version mismatch was demonstrated. The old CI run already used Chrome/driver 155.0.8059.84, so pinning this build controls drift rather than claiming a browser upgrade fixed the failures. The optional missing-CDP-module warning also occurred in passing local runs; these tests use W3C WebDriver and ChromeDriver's performance log, not Selenium version-specific DevTools bindings.

## Controlled executions

All runs below execute the same source commit **3fa54f3a3a64a27adcb22b40611fb9ac5d5a8dc0** with the same pinned browser, assertions, timeout values and headless settings. Runs were dispatched sequentially; none was cancelled or automatically retried. The affected scope selects exactly authentication:28, cart:16 and registration:34, and Maven still executes all eight component checks. Full scope applies no Cucumber feature filter.

| Hosted execution                                                                                                 | Workers configured / observed | Live passed/total | Components passed/total | Passed | Failed | Skipped | Status  |
| ---------------------------------------------------------------------------------------------------------------- | ----------------------------: | ----------------: | ----------------------: | -----: | -----: | ------: | ------- |
| [affected-2workers](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065030757)   |                         2 / 2 |               3/3 |                     8/8 |     11 |      0 |       0 | success |
| [full-2workers](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065187643)       |                         2 / 2 |             30/30 |                     8/8 |     38 |      0 |       0 | success |
| [full-1worker](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065491628)        |                         1 / 1 |             30/30 |                     8/8 |     38 |      0 |       0 | success |
| [full-1worker-repeat](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065942526) |                         1 / 1 |             30/30 |                     8/8 |     38 |      0 |       0 | success |

Both complete one-worker validations were planned reproducibility checks, not automatic retries of failed tests. Each full report was cross-checked against Surefire XML and the same 30 feature/example coordinates and eight component method names from the baseline. The result JSON records preserve failed counts if any run fails; no outcomes are manufactured. The brief local discovery dry-run was explicitly marked dry-run and is not counted as UI execution evidence. A real local execution of the three affected examples with diagnostics enabled passed 3/3 before CI.

## Resource comparison

| Full run            | CPU mean | CPU peak | Minimum available RAM | Peak summed Chrome RSS | Live-run duration |
| ------------------- | -------: | -------: | --------------------: | ---------------------: | ----------------: |
| full-2workers       |   71.24% |   85.00% |             12.33 GiB |               4.74 GiB |             153 s |
| full-1worker        |   35.69% |   67.84% |             13.07 GiB |               2.48 GiB |             253 s |
| full-1worker-repeat |   34.95% |   63.54% |             13.19 GiB |               2.15 GiB |             207 s |

CPU values are whole-runner five-second samples during live scenarios, not per-renderer profiles or subsecond maxima. Summed RSS double-counts shared pages and is not physical RAM consumption. The two-worker runner had at least 12.33 GiB available; startup evidence also showed about 8 GiB shared-memory capacity and over 84 GiB free temporary-disk capacity. The existing disable-dev-shm-usage flag was identical in all runs. There was no sustained RAM/disk depletion in the measured successful runs. Cgroup OOM counters were unavailable on these VMs and are explicitly null/unavailable in the summaries, not reported as zero OOM events.

One worker reduced mean CPU from 71.24% to 35.69% and peak summed Chrome RSS from 4.74 to 2.48 GiB in the first comparison. It gives a smaller runner more headroom, at the cost of elapsed time. This is an operational choice, **not proof that parallelism caused the old timeouts**. Different runner VMs, observation windows and public-site conditions limit causal conclusions. [Measured local environment](local-environment.json) and each run's JSON retain the source values.

## Targeted changes

- CI defaults to **one scenario worker**. Local POM/properties remain at two; a manual input still permits a measured two-worker CI run.
- CI Chrome/driver is pinned to **155.0.8059.84** for reproducible comparisons. Update the pin deliberately and revalidate before adopting another build.
- The workflow offers explicit affected/full inputs, preserves the original Maven exit status, uploads actual artifacts on failure, and queues runs rather than interrupting active cleanup.
- Opt-in ci.diagnostics enables ChromeDriver performance observations. A scenario after-hook writes only whitelisted network/status/timing/frame fields. Headers, cookies, query strings, request bodies and tokens are excluded; there is no interception or response modification.
- A CI-only Python standard-library sampler records host resources every five seconds and one normal HTTPS observation before/after execution. Its sampling interval never synchronizes Selenium actions. Java framework execution locally does not require Python.

Only workflow/diagnostics and documentation changed. Feature files, Page Objects, step definitions, component checks, test data, assertions and Maven test discovery are unchanged. Existing public report snapshots and their links remain intact. A final documentation-only commit uses [skip ci] to avoid a redundant public-demo regression; the linked completed runs validate the executable source/workflow.

## HTML report verification

I downloaded the final hosted artifact and rendered its Extent, Cucumber and Surefire index reports in Chromium over a temporary local HTTP report viewer. Extent displayed the live scenario results, Cucumber displayed 100% passed, and Surefire displayed 38 methods, 38 passed. This report inspection does not run or host the application under test.

## Final status and limits

The [final completed full CI run](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065942526) reports **38 passed, 0 failed, 0 skipped**, covering all 38 existing tests. The earlier 35/38 failure and its screenshots remain retained. A future origin 525 will still fail a real UI scenario: this framework does not conceal external outages. Exact origin-side TLS configuration and historical renderer causality remain outside what the captured baseline can establish.
