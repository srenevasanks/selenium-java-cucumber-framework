# Local IDE setup

Use Eclipse IDE for Java Developers or another Java IDE with Maven support. Java 17, Maven 3.9+ and Chrome are required. No Cucumber editor plugin is required to execute the tests.

## Eclipse import

1. File → Import → Maven → Existing Maven Projects.
2. Select the extracted project's directory containing `pom.xml`.
3. Finish dependency resolution.
4. Window → Preferences → Java → Installed JREs: add a **Standard VM** pointing to JDK 17.
5. Set the project JRE system library and compiler level to Java 17.
6. Right-click project → Maven → Update Project if necessary.

## Execute

Use Run → Run Configurations → Maven Build → New:

- Base directory: `${workspace_loc:/selenium-java-cucumber-framework}`
- Goals: `clean test -Dheadless=false` to display Chrome (use `clean test` for headless execution)
- JRE: JDK 17

Visible smoke goals: `clean test -Dcucumber.filter.tags=@smoke -Dheadless=false`.

Parallel goals: `clean test -Dscenarios.parallel=true -Dthreads=2`.

The included `Smoke - Visible Chrome.launch` and `Regression - Headless.launch` files provide equivalent Maven goals. If the IDE imported the project under a different name, update their base directory accordingly.

Open `reports/extent-report.html` in Chrome after execution. Maven/Console and `target/surefire-reports` provide additional diagnostics.

For exact paths on the configured Windows computer, see [RUN-ME.md](../RUN-ME.md).

Parallel execution is enabled by default with 2 scenario workers. Use goals `clean test -Dheadless=false` in Eclipse to watch the parallel browsers, or `clean test -Dscenarios.parallel=false -Dheadless=false` for one browser at a time. Remove an old `-Dthreads=8` override if present. GitHub Actions uses 2 workers.
