package com.github.aiverifier.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerificationReport;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.KarateRunner;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Slf4j
public class DefaultKarateRunner implements KarateRunner {

    private static final String KARATE_CONFIG_FILE = "karate-config.js";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VerificationReport run(Path featurePath, VerifierConfig config) {
        log.info("Running Karate feature: {}", featurePath);

        String reportDir = "./target/ai-verifier/reports/karate";
        if (config.getKarate() != null && config.getKarate().getReportDir() != null) {
            reportDir = config.getKarate().getReportDir();
        }

        try {
            writeKarateConfig(featurePath.getParent(), config);

            Results results = Runner.path(featurePath.toString())
                    .systemProperty("karate.config.dir", featurePath.getParent().toString())
                    .karateEnv("default")
                    .outputCucumberJson(true)
                    .outputHtmlReport(true)
                    .reportDir(reportDir)
                    .parallel(1);

            return buildReport(results, featurePath);
        } catch (Exception e) {
            throw new VerifierException("Karate execution failed: " + e.getMessage(), 6, e);
        }
    }

    private void writeKarateConfig(Path directory, VerifierConfig config) throws IOException {
        Files.createDirectories(directory);
        String content = """
                function fn() {
                  return {
                    baseUrl: %s,
                    testData: %s,
                    database: %s,
                    authFlow: %s,
                    db: new (Java.type('com.github.aiverifier.impl.KarateDb'))(%s)
                  };
                }
                """.formatted(
                toJson(config.getApp().getBaseUrl()),
                toJson(config.getTestData() != null ? config.getTestData() : java.util.Map.of()),
                toJson(toDatabaseConfig(config)),
                toJson(toAuthFlowConfig(config)),
                toJson(toDatabaseConfig(config)));

        Files.writeString(directory.resolve(KARATE_CONFIG_FILE), content);
        log.info("Saved Karate runtime config: {}", directory.resolve(KARATE_CONFIG_FILE));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new VerifierException("Failed to serialize Karate runtime config: " + e.getMessage(), 6, e);
        }
    }

    private java.util.Map<String, Object> toDatabaseConfig(VerifierConfig config) {
        if (config.getDatabase() == null) {
            return java.util.Map.of("enabled", false);
        }

        java.util.Map<String, Object> database = new java.util.LinkedHashMap<>();
        database.put("enabled", config.getDatabase().isEnabled());
        database.put("type", config.getDatabase().getType());
        database.put("jdbcUrl", config.getDatabase().getJdbcUrl());
        database.put("username", config.getDatabase().getUsername());
        database.put("password", config.getDatabase().getPassword());
        database.put("readonly", config.getDatabase().isReadonly());
        return database;
    }

    private java.util.Map<String, Object> toAuthFlowConfig(VerifierConfig config) {
        if (config.getAuthFlow() == null) {
            return java.util.Map.of();
        }

        java.util.Map<String, Object> authFlow = new java.util.LinkedHashMap<>();
        authFlow.put("registrationEndpoint", config.getAuthFlow().getRegistrationEndpoint());
        authFlow.put("confirmationEndpoint", config.getAuthFlow().getConfirmationEndpoint());
        authFlow.put("tokenEndpoint", config.getAuthFlow().getTokenEndpoint());
        authFlow.put("notificationQuery", config.getAuthFlow().getNotificationQuery());
        authFlow.put("confirmationLinkColumn", config.getAuthFlow().getConfirmationLinkColumn());
        authFlow.put("confirmationTokenRegex", config.getAuthFlow().getConfirmationTokenRegex());
        authFlow.put("accessTokenJsonPath", config.getAuthFlow().getAccessTokenJsonPath());
        authFlow.put("refreshTokenJsonPath", config.getAuthFlow().getRefreshTokenJsonPath());
        return authFlow;
    }

    private VerificationReport buildReport(Results results, Path featurePath) {
        VerificationReport report = new VerificationReport();
        report.setTimestamp(Instant.now());
        report.setGeneratedFeaturePath(featurePath.toString());

        if (results.getFailCount() == 0) {
            report.setStatus(VerificationReport.Status.PASSED);
            log.info("Karate execution PASSED ({} scenarios)", results.getScenariosTotal());
        } else {
            report.setStatus(VerificationReport.Status.FAILED);
            log.warn("Karate execution FAILED ({} of {} scenarios failed)",
                    results.getScenariosFailed(), results.getScenariosTotal());
        }

        report.getPassed().add(results.getScenariosPassed() + " scenarios passed");
        if (results.getFailCount() > 0) {
            report.getFailed().add(results.getScenariosFailed() + " scenarios failed");
        }

        return report;
    }
}
