# Verified GitHub Actions execution — 18 October 2026 (IST)

The remaining hosted failures were investigated without rebuilding the framework or changing its business assertions. [Detailed findings, exact failures, screenshots and resource comparison](ci-investigation-2026-10-18/README.md).

| Hosted execution                                                                                                 | Workers configured / observed | Live passed/total | Components passed/total | Passed | Failed | Skipped | Status  |
| ---------------------------------------------------------------------------------------------------------------- | ----------------------------: | ----------------: | ----------------------: | -----: | -----: | ------: | ------- |
| [affected-2workers](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065030757)   |                         2 / 2 |               3/3 |                     8/8 |     11 |      0 |       0 | success |
| [full-2workers](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065187643)       |                         2 / 2 |             30/30 |                     8/8 |     38 |      0 |       0 | success |
| [full-1worker](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065491628)        |                         1 / 1 |             30/30 |                     8/8 |     38 |      0 |       0 | success |
| [full-1worker-repeat](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065942526) |                         1 / 1 |             30/30 |                     8/8 |     38 |      0 |       0 | success |

The final [one-worker complete run](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39065942526) executed all **30 live Cucumber scenarios + 8 browser component tests**. Both complete one-worker validations are recorded above. Source row/method identities and actual JSON counts agree with Surefire XML. All executions used real Chrome against Automation Exercise; no mocked result is presented as integration evidence.

CI now defaults to one worker, with two-worker local execution retained. The full two-worker CI run also passed. Observed CPU/memory headroom supports the conservative CI limit; it does not prove the original failures were caused by parallelism.

The old 525 screenshot is an upstream Cloudflare-to-origin TLS error. The two native renderer timeouts were not reproduced in the controlled runs. Their precise historical cause is unproven; no assertion, timeout or page-load strategy was changed. [Original failing run](https://github.com/srenevasanks/selenium-java-cucumber-framework/actions/runs/39052648633), [17 October review](execution-summary-2026-10-17.md) and [15 October history](execution-summary-2026-10-15.md) remain preserved.

Runtime reports are target/test-output/extent-report.html and cucumber.html for live scenarios, target/test-output/execution-summary.json for separate result categories, and target/surefire-reports/index.html for all tests. Existing published Extent/Cucumber links remain the frozen 15 October snapshot. Current run artifacts are attached to each GitHub Actions execution.

No retries, sleeps for synchronization, excessive timeouts, disabled tests, Cloudflare workarounds or fabricated results were introduced. The final documentation-only commit does not repeat the public-demo suite; all executable changes are validated by the linked completed runs.
