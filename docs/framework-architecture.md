# Framework architecture

The framework separates business examples, test orchestration and browser behavior.

```mermaid
flowchart TD
  Maven[Maven Surefire - Java 17] --> Runner[RunCucumberTest - TestNG]
  Maven --> Fixtures[AdvertisementTest - browser component checks]
  Features[Six Gherkin feature files] --> Runner
  Runner --> Provider[Sequential OR parallel data provider]
  Provider --> Steps[Scenario-local StoreSteps + TestNG assertions]
  Provider --> Hooks[BrowserHooks]
  Steps --> Pages[Page Object Model]
  Pages --> Components[Header + Advertisement components]
  Pages --> Driver[DriverManager - ThreadLocal WebDriver]
  Hooks --> Driver
  Fixtures --> Driver
  Fixtures --> Loopback[Self-owned loopback HTML fixtures]
  Steps --> Data[UUID Customer + fixed JSON expectations + BigDecimal]
  Hooks --> Capture[Failure / E2E PNG attachments]
  Runner --> Events[Cucumber events keyed by scenario UUID]
  Capture --> Events
  Events --> Extent[Extent + Cucumber reports + Cucumber event summary]
  Maven --> Summary[TestNG ExecutionSummaryReporter]
  Summary --> Counts[Separate live / component / other / configuration counts]
  Extent --> Runtime[Ignored target/test-output]
  Counts --> Runtime
  Maven --> Surefire[target/surefire-reports - all tests]
  Snapshot[Frozen reports snapshot - 15 October] --> PagesSite[Existing direct HTML links]
```

## Responsibilities

- Configuration loads properties once; JVM properties override defaults.
- DriverManager owns Chrome creation, optional headless/remote execution and teardown. ThreadLocal storage prevents cross-thread driver sharing.
- Cucumber creates separate step-definition/hook instances per scenario. User credentials and registration state remain scenario-local.
- Page objects own locators, interaction and explicit waits. The base page centralizes navigation, input, rendered-text normalization and native browser validation inspection.
- Both links and inputs are centered with instant scrolling, followed by a read-only DOM hit test at the target center. This avoids acting during smooth scroll animation or through a fixed banner. Clear, typing and clicks remain native WebDriver operations. A dismissed ad that consumed a link click permits one continuation of that same link only while the browser remains on the original page; form submissions and whole scenarios are not retried.
- Header is a reusable navigation component. Advertisement handles only ordinary vignette close controls. Native WebDriver clicks remain in use; JavaScript is limited to scrolling and read-only HTML validity inspection.
- Ad checks run before element interactions and after native clicks, including non-navigation actions such as expanding a category. A bounded traversal looks for the observed Close control only inside known Advertisement frames and their descendants. Frame context is restored after inspection. A click intercepted by a recognized vignette waits for dismissal; unrelated interception still fails. This is interaction synchronization, not a scenario retry.
- Close targets must have a positive rendered area; a visible child of a zero-sized control wrapper can receive the native click. Active-ad detection checks visible frame geometry and usable Close controls because Google's URL fragment can remain after the overlay has gone. A large visible advertising frame remains blocking while its controls load. The application must still satisfy its normal navigation and business assertions after dismissal.
- JSON products are fixed expected data. Decimal money parsing avoids binary floating-point rounding.
- Steps express workflow and meaningful assertions. They do not contain raw Selenium element operations.
- Cleanup runs after evidence capture and before browser teardown. It deletes only the unique disposable account created by that scenario, using the UI and in-memory credentials. Cleanup errors are reported.
- Extent is populated only by Cucumber events. UUID-keyed concurrent mappings and atomic counters support parallel scenarios. Each scenario has its own Extent test; final flushing occurs after the Cucumber run finishes.
- `ExecutionSummaryReporter` is registered as a TestNG listener through Surefire. It groups actual passed/failed/skipped result sets by test class after suite execution, without adding component results to Extent. Configuration failures/skips remain separate. When Cucumber executes, its event counts are checked against the corresponding TestNG results in the JSON output. The listener does not change test statuses or swallow assertion failures.
- At execution start, the listener removes known stale Cucumber HTML/JSON files from the ignored runtime directory. A component-only invocation therefore cannot present an old Cucumber run as current. It never touches curated reports.
- Local driver startup writes one WARNING-level ChromeDriver log per session under the ignored runtime directory. If timeout configuration fails after session creation, DriverManager attempts teardown, removes the ThreadLocal, and preserves cleanup exceptions as suppressed on the original setup error.

## Execution

The TestNG runner exposes sequential and parallel data providers. The `scenarios.parallel` property selects exactly one. The `threads` property controls parallel data-provider concurrency.

Defaults are `scenarios.parallel=true` and `threads=2`; GitHub Actions defaults to 1 worker, with an optional manual 2-worker comparison. Maven `-T` is not the scenario scheduler. Component checks execute separately from the Cucumber provider. Extent uses atomic active/peak counters to record observed concurrent scenarios as well as the configured limit; the summary JSON exposes both values for verification.

Each scenario opens a new browser. There are no static accounts, dependent scenarios or automatic test retries. A public-site failure is visible in Maven's exit code, Cucumber and Extent.

Screenshots are captured before cleanup when a step fails. An additional teardown check captures cleanup failures. Successful E2E journeys attach their actual order-confirmation screenshot.

## Tradeoffs

UI account creation keeps setup transparent and verifies actual registration, at the cost of execution time. The shared public application and advertising may change independently of this repository. Locators prefer observed IDs, `data-qa` attributes and scoped CSS selectors. There are no invented service layers, unused exception hierarchies or security-bypass mechanisms.

## Ad-component regression checks

`components.AdvertisementTest` adds eight deterministic Selenium checks alongside the live Cucumber suite. A JDK HttpServer binds only to loopback on an ephemeral port and serves small, self-owned fixtures. Native Close clicks must update observable fixture state and allow the next page action. The nested case also proves restoration of the main frame context. The late-ad case proves that post-click synchronization dismisses an overlay triggered without navigation. These are component checks, not substitutes for live application scenarios; they appear in TestNG results rather than Extent's Cucumber totals.

Two fixtures also verify continuation of an ad-consumed link and native field editing across a smooth-scrolling page with a fixed bottom banner. The fixture Close handlers remove their own fixture overlays; the framework never removes public-site ad DOM nodes.

## Artifact lifecycle

Runtime reports, screenshots and logs are written only beneath `target/test-output/`; Surefire uses `target/surefire-reports/`. Both are ignored and reset by `mvn clean`. TestNG generates the consolidated summary even after ordinary test failures; a JVM crash or failure before TestNG starts may prevent report generation and is not represented as an empty passing run.

`reports/` retains the original dated, passing snapshot and historical repeatability evidence. It is not a runtime destination. Existing GitHub Pages URLs continue to open Extent and Cucumber directly, with no separate site landing page. Current review summaries live under a dated documentation folder. CI artifacts contain the actual run's runtime output, not the frozen snapshot.

## CI-only diagnostics

With ci.diagnostics=true, DriverManager enables ChromeDriver performance logs and BrowserHooks calls CiNetworkDiagnostics before evidence/cleanup. It writes whitelisted status/timing/frame data, excluding headers, cookies, query strings and request bodies. It does not intercept requests or change scenario assertions. The workflow adds a read-only Python resource sampler and ordinary verified HTTPS observations. These CI tools are not a replacement execution architecture and are not required for local Java runs. See the [dated CI investigation](ci-investigation-2026-10-18/README.md).
