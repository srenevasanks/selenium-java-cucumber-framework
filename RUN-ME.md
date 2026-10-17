# Run this project on your computer

The working project folder is:

```text
C:\Users\Admin\Documents\Codex\2026-10-10\project-production-quality-selenium-java-cucumber\outputs\selenium-java-cucumber-framework
```

## Option 1 — PowerShell

1. Open **PowerShell**.
2. Copy and run:

```powershell
cd "C:\Users\Admin\Documents\Codex\2026-10-10\project-production-quality-selenium-java-cucumber\outputs\selenium-java-cucumber-framework"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
mvn -version
```

3. Confirm Maven reports Java **17**.
4. Run all tests:

```powershell
mvn clean test
```

5. Wait for the final Maven result. `BUILD SUCCESS` means the selected scenarios passed; `BUILD FAILURE` means you should inspect the report.
6. Open the generated report:

```powershell
Start-Process ".\target\test-output\extent-report.html"
```

To watch Chrome execute the eight smoke scenarios:

```powershell
mvn clean test "-Dcucumber.filter.tags=@smoke" "-Dheadless=false"
```

To run only cart tests:

```powershell
mvn clean test "-Dcucumber.filter.tags=@cart"
```

Parallel execution uses up to two Chrome sessions by default. To set that limit explicitly:

```powershell
mvn clean test "-Dscenarios.parallel=true" "-Dthreads=2"
```

The `JAVA_HOME` assignment affects only this PowerShell session. If `mvn` is not found, use the installed executable in place of `mvn`:

```powershell
& "C:\Users\Admin\Downloads\apps\apache-maven-3.9.11\bin\mvn.cmd" clean test
```

No manual account preparation or payment data is needed.

Ordinary ad popups may appear briefly. Leave the automated Chrome window open; the framework closes the ad itself. The full command also runs eight short browser component checks on local browser fixtures, followed by the public-site scenarios. These component checks are not a local installation of the store.

## Option 2 — Eclipse

1. Start Eclipse and choose the existing `work\eclipse-workspace` workspace.
2. Select `selenium-java-cucumber-framework` in Package Explorer and press **F5** to refresh it.
3. Right-click the project → **Maven → Update Project** if Eclipse shows stale Java errors.
4. Open **Run → Run Configurations → Maven Build**.
5. Create a configuration named **Automation Exercise - All Tests**.
6. Set **Base directory** to `${workspace_loc:/selenium-java-cucumber-framework}`.
7. Set **Goals** to `clean test -Dheadless=false` to see Chrome.
8. In the **JRE** tab select the configured **jdk-17**.
9. Click **Apply**, then **Run**.
10. After completion, open `target/test-output/extent-report.html` from File Explorer in Chrome.

For visible smoke tests, use goals `clean test -Dcucumber.filter.tags=@smoke -Dheadless=false`. The project also includes portable smoke and regression `.launch` files.

Do not launch an old **Public Sites - Selenium Assessment** configuration: that was a comparison probe, not this portfolio suite.

Parallel execution is enabled by default with 2 scenario workers. Use goals `clean test -Dheadless=false` in Eclipse to watch the parallel browsers, or `clean test -Dscenarios.parallel=false -Dheadless=false` for one browser at a time. Remove an old `-Dthreads=8` override if present. GitHub Actions uses 2 workers.
