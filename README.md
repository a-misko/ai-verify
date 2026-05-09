# AI Backend Runtime Verifier

AI Backend Runtime Verifier is a CLI utility that uses an AI coding agent to generate a Karate runtime verification scenario from a task description and the current Git diff.

The tool is intended for backend changes that should be checked against a real locally running application. It does not modify production code. It generates a `.feature` file, validates it against safety rules, runs it with Karate, and writes Markdown and JSON reports.

## How It Works

1. Loads `ai-verifier.yml` with project, application, Git, AI provider, Karate, and security settings.
2. Loads `scenario.yml` with the task and expected behavior to verify.
3. Checks that the configured application endpoint is reachable.
4. Collects the Git diff from the target project.
5. Builds a prompt for the selected AI provider.
6. Calls either `claude-code` or `codex-code`.
7. Extracts a Karate `.feature` file from the AI response.
8. Validates the generated scenario against configured security constraints.
9. Runs Karate and writes reports.

## Build

```bash
mvn package
```

The shaded CLI jar is produced as:

```text
ai-verifier-launcher/target/ai-verifier.jar
```

## Configuration Example

`ai-verifier.yml`:

```yaml
project:
  path: /path/to/backend-project
  language: java
  framework: spring-boot

git:
  diffCommand: git diff main...HEAD

app:
  baseUrl: http://localhost:8080
  healthUrl: http://localhost:8080/actuator/health

database:
  enabled: true
  type: postgres
  jdbcUrl: jdbc:postgresql://localhost:5432/app
  username: app
  password: app
  readonly: true

ai:
  provider: codex-code # supported: claude-code, codex-code

codexCode:
  command: codex
  timeoutSeconds: 180
  model: gpt-5.3-codex

claudeCode:
  command: claude
  timeoutSeconds: 180

karate:
  outputDir: ./target/ai-verifier/generated
  reportDir: ./target/ai-verifier/reports

security:
  forbiddenMethods:
    - delete
    - patch
  allowedHosts:
    - localhost
  sqlReadonlyOnly: true
  allowShellExecutionInGeneratedScenario: false
```

If `ai.provider` is omitted, `claude-code` is used by default.

When `database.enabled` is true, the generated Karate scenario can use the `database` variable for read-only JDBC checks and bounded polling. PostgreSQL and MySQL drivers are bundled in the CLI jar.

Authentication is expected to be created inside each generated scenario: register a unique user, read the confirmation link from the `notifications` table, confirm the account, obtain access and refresh tokens, and store them as Karate variables for later requests.

## Default AI Prompt

The default AI instructions are stored in:

```text
ai-verifier-impl/src/main/resources/prompts/default-ai-prompt.md
```

Edit this file to change the shared prompt rules. The current scenario, runtime config, test data, and Git diff are appended by the application at runtime.

## Scenario Example

`scenario.yml`:

```yaml
task:
  id: BE-1247
  title: Add user profile display name update
  description: >
    Implement an endpoint that allows an authenticated user to update their
    profile display name. The endpoint must persist the new display name,
    return the updated profile, reject blank or too long values, and must not
    allow one user to update another user's profile.

verificationRequest:
  focus:
    - Find all endpoints affected by task BE-1247.
    - Verify the successful display name update flow.
    - Verify validation errors for blank and oversized display names.
    - Verify that cross-user profile updates are forbidden.

expectedBehavior:
  - A valid authenticated request returns a 2xx status with the updated profile.
  - The updated display name is persisted and visible on the next profile read.
  - Blank display names return a 4xx status with a readable validation error.
  - Display names longer than the allowed limit return a 4xx status.
  - A user cannot update another user's profile.

constraints:
  destructiveOperationsAllowed: false
  createNewEntitiesAllowed: false
  useExistingTestData: true
```

## Usage

Start the backend application locally, then run:

```bash
java -jar ai-verifier-launcher/target/ai-verifier.jar \
  --config ai-verifier.yml \
  --scenario scenario.yml
```

Generated files and reports are written to the configured `karate.outputDir` and `karate.reportDir`.
