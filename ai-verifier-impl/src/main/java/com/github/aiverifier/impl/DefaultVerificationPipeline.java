package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.model.ValidationResult;
import com.github.aiverifier.core.model.VerificationReport;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DefaultVerificationPipeline implements VerificationPipeline {
    private final ConfigLoader configLoader;
    private final ScenarioLoader scenarioLoader;
    private final EnvironmentChecker environmentChecker;
    private final GitDiffCollector gitDiffCollector;
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

            // Step 4: Build prompt
            log.info("=== Step 4: Building prompt ===");
            String prompt = promptBuilder.buildPrompt(config, scenario, gitDiff);
            saveFile(Path.of(outputDir, taskId + ".prompt.md"), prompt);

            // Step 5: Call AI
            log.info("=== Step 5: Calling AI provider ===");
            AiProvider aiProvider = aiProviderFactory.create(config);
            int timeout = aiProviderFactory.resolveTimeoutSeconds(config);
            String aiResponse = aiProvider.generate(prompt, config.getProject().getPath(), timeout);
            saveFile(Path.of(outputDir, taskId + ".ai-response.txt"), aiResponse);

            // Step 6: Extract feature
            log.info("=== Step 6: Extracting feature ===");
            String featureContent = featureExtractor.extractFeature(aiResponse);
            Path featurePath = Path.of(outputDir, taskId + ".generated.feature");
            saveFile(featurePath, featureContent);

            // Step 7: Validate feature
            log.info("=== Step 7: Validating feature ===");
            ValidationResult validation = featureValidator.validate(featureContent, config.getSecurity());
            if (!validation.isValid()) {
                log.error("Feature validation failed:");
                validation.getViolations().forEach(v -> log.error("  - {}", v));
                return 5;
            }

            // Step 8: Run Karate
            log.info("=== Step 8: Running Karate ===");
            VerificationReport report = karateRunner.run(featurePath, config);
            report.setTaskId(taskId);

            // Step 9: Write reports
            log.info("=== Step 9: Writing reports ===");
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
}
