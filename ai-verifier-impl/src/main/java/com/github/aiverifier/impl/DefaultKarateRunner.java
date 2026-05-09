package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerificationReport;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.KarateRunner;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DefaultKarateRunner implements KarateRunner {
    @Override
    public VerificationReport run(Path featurePath, VerifierConfig config) {
        log.info("Running Karate feature: {}", featurePath);

        Map<String, Object> vars = new HashMap<>();
        vars.put("baseUrl", config.getApp().getBaseUrl());
        if (config.getAuth() != null && config.getAuth().getToken() != null) {
            vars.put("bearerToken", config.getAuth().getToken());
        }
        if (config.getTestData() != null) {
            vars.put("testData", config.getTestData());
        }

        String reportDir = "./target/ai-verifier/reports/karate";
        if (config.getKarate() != null && config.getKarate().getReportDir() != null) {
            reportDir = config.getKarate().getReportDir();
        }

        try {
            Results results = Runner.path(featurePath.toString())
                    .systemProperty("karate.config.dir", featurePath.getParent().toString())
                    .karateEnv("default")
                    .outputCucumberJson(true)
                    .outputHtmlReport(true)
                    .reportDir(reportDir)
                    .systemProperty("baseUrl", config.getApp().getBaseUrl())
                    .parallel(1);

            return buildReport(results, featurePath);
        } catch (Exception e) {
            throw new VerifierException("Karate execution failed: " + e.getMessage(), 6, e);
        }
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
