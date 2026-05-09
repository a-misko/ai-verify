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

## Scenario Example

`scenario.yml`:

```yaml
task:
  id: user-profile-update
  title: Verify user profile update
  description: Ensure the profile update endpoint persists valid display name changes.

verificationRequest:
  focus:
    - Validate successful profile update response.
    - Verify invalid input is rejected.

expectedBehavior:
  - A valid request returns a 2xx status.
  - Invalid display names return a 4xx status.

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
