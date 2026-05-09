package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.ProjectInventory;
import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.model.ValidationResult;
import com.github.aiverifier.core.model.VerificationReport;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.*;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DefaultVerificationPipeline implements VerificationPipeline {
    private final ConfigLoader configLoader;
    private final ScenarioLoader scenarioLoader;
    private final EnvironmentChecker environmentChecker;
    private final GitDiffCollector gitDiffCollector;
    private final ProjectInventoryCollector projectInventoryCollector;
    private final AuthFlowResolver authFlowResolver;
    private final PromptBuilder promptBuilder;
    private final AiProviderFactory aiProviderFactory;
    private final FeatureExtractor featureExtractor;
    private final FeatureValidator featureValidator;
    private final KarateRunner karateRunner;
    private final ReportWriter reportWriter;

    public DefaultVerificationPipeline(
            ConfigLoader configLoader,
            ScenarioLoader scenarioLoader,
            EnvironmentChecker environmentChecker,
            GitDiffCollector gitDiffCollector,
            ProjectInventoryCollector projectInventoryCollector,
            AuthFlowResolver authFlowResolver,
            PromptBuilder promptBuilder,
            AiProviderFactory aiProviderFactory,
            FeatureExtractor featureExtractor,
            FeatureValidator featureValidator,
            KarateRunner karateRunner,
            ReportWriter reportWriter) {
        this.configLoader = configLoader;
        this.scenarioLoader = scenarioLoader;
        this.environmentChecker = environmentChecker;
        this.gitDiffCollector = gitDiffCollector;
        this.projectInventoryCollector = projectInventoryCollector;
        this.authFlowResolver = authFlowResolver;
        this.promptBuilder = promptBuilder;
        this.aiProviderFactory = aiProviderFactory;
        this.featureExtractor = featureExtractor;
        this.featureValidator = featureValidator;
        this.karateRunner = karateRunner;
        this.reportWriter = reportWriter;
    }

    @Override
    public int execute(Path configPath, Path scenarioPath) {
        try {
            // Step 1: Load config and scenario
            VerifierConfig config = configLoader.load(configPath);
            ScenarioRequest scenario = scenarioLoader.load(scenarioPath);
            String taskId = scenario.getTask().getId();

            // Resolve output directories
            String outputDir = resolveOutputDir(config);
            String reportDir = resolveReportDir(config);
            Files.createDirectories(Path.of(outputDir));
            Files.createDirectories(Path.of(reportDir));

            // Step 2: Check environment
            log.info("=== Step 2: Checking environment ===");
            environmentChecker.check(config);

            // Step 3: Collect git diff
            log.info("=== Step 3: Collecting git diff ===");
            String gitDiff = gitDiffCollector.collectDiff(config);
            saveFile(Path.of(outputDir, taskId + ".git-diff.patch"), gitDiff);

            // Step 4: Build project inventory
            log.info("=== Step 4: Building project inventory ===");
            ProjectInventory inventory = projectInventoryCollector.collect(config, scenario, gitDiff);
            saveFile(Path.of(outputDir, taskId + ".project-inventory.md"), formatInventory(inventory));

            AiProvider aiProvider = aiProviderFactory.create(config);
            int timeout = aiProviderFactory.resolveTimeoutSeconds(config);

            // Step 5: Resolve auth flow
            log.info("=== Step 5: Resolving auth flow ===");
            VerifierConfig.AuthFlowConfig authFlow = authFlowResolver.resolve(
                    config, scenario, gitDiff, inventory, aiProvider, timeout);
            config.setAuthFlow(authFlow);
            saveFile(Path.of(outputDir, taskId + ".auth-flow.json"), toJson(authFlow));

            // Step 6: Build prompt
            log.info("=== Step 6: Building prompt ===");
            String prompt = promptBuilder.buildPrompt(config, scenario, gitDiff, inventory);
            saveFile(Path.of(outputDir, taskId + ".prompt.md"), prompt);

            // Step 7: Call AI
            log.info("=== Step 7: Calling AI provider ===");
            String aiResponse = aiProvider.generate(prompt, config.getProject().getPath(), timeout);
            saveFile(Path.of(outputDir, taskId + ".ai-response.txt"), aiResponse);

            // Step 8: Extract feature
            log.info("=== Step 8: Extracting feature ===");
            String featureContent = featureExtractor.extractFeature(aiResponse);
            Path featurePath = Path.of(outputDir, taskId + ".generated.feature");
            saveFile(featurePath, featureContent);

            // Step 9: Validate feature
            log.info("=== Step 9: Validating feature ===");
            ValidationResult validation = featureValidator.validate(featureContent, config.getSecurity());
            if (!validation.isValid()) {
                log.error("Feature validation failed:");
                validation.getViolations().forEach(v -> log.error("  - {}", v));
                return 5;
            }

            // Step 10: Run Karate
            log.info("=== Step 10: Running Karate ===");
            VerificationReport report = karateRunner.run(featurePath, config);
            report.setTaskId(taskId);
            enrichReport(report, scenario, inventory);

            // Step 11: Write reports
            log.info("=== Step 11: Writing reports ===");
            reportWriter.writeMarkdown(report, Path.of(reportDir, taskId + ".report.md"));
            reportWriter.writeJson(report, Path.of(reportDir, taskId + ".report.json"));

            log.info("=== Verification complete: {} ===", report.getStatus());
            return report.getStatus() == VerificationReport.Status.PASSED ? 0 : 1;

        } catch (VerifierException e) {
            log.error("Verification failed: {}", e.getMessage());
            return e.getExitCode();
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return 1;
        }
    }

    private String resolveOutputDir(VerifierConfig config) {
        if (config.getKarate() != null && config.getKarate().getOutputDir() != null) {
            return config.getKarate().getOutputDir();
        }
        return "./target/ai-verifier/generated";
    }

    private String resolveReportDir(VerifierConfig config) {
        if (config.getKarate() != null && config.getKarate().getReportDir() != null) {
            return config.getKarate().getReportDir();
        }
        return "./target/ai-verifier/reports";
    }

    private void saveFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            log.info("Saved: {}", path);
        } catch (IOException e) {
            log.warn("Failed to save file: {}", path, e);
        }
    }

    private String toJson(Object value) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new VerifierException("Failed to serialize generated artifact: " + e.getMessage(), 4, e);
        }
    }

    private String formatInventory(ProjectInventory inventory) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "Affected Endpoints", inventory.getAffectedEndpoints());
        appendSection(sb, "Relevant Source Files", inventory.getRelevantSourceFiles());
        appendSection(sb, "Database Artifacts", inventory.getDatabaseArtifacts());
        appendSection(sb, "Messaging Artifacts", inventory.getMessagingArtifacts());
        appendSection(sb, "Auth Artifacts", inventory.getAuthArtifacts());
        appendSection(sb, "Notes", inventory.getNotes());
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, java.util.List<String> values) {
        sb.append("## ").append(title).append("\n");
        if (values == null || values.isEmpty()) {
            sb.append("- none detected\n\n");
            return;
        }
        values.forEach(value -> sb.append("- ").append(value).append("\n"));
        sb.append("\n");
    }

    private void enrichReport(VerificationReport report, ScenarioRequest scenario, ProjectInventory inventory) {
        report.getAffectedEndpoints().addAll(inventory.getAffectedEndpoints());
        report.getDbChecks().addAll(inventory.getDatabaseArtifacts());
        report.getAsyncChecks().addAll(inventory.getMessagingArtifacts());
        if (scenario.getExpectedBehavior() != null) {
            scenario.getExpectedBehavior().forEach(value -> report.getCoverage().add("Expected behavior requested: " + value));
        }
        if (inventory.getAffectedEndpoints().isEmpty()) {
            report.getNotVerifiedReasons().add("No affected endpoints were detected by static inventory.");
        }
        if (inventory.getDatabaseArtifacts().isEmpty()) {
            report.getNotVerifiedReasons().add("No database artifacts were detected by static inventory.");
        }
        report.getNotVerified().addAll(report.getNotVerifiedReasons());
    }
}
