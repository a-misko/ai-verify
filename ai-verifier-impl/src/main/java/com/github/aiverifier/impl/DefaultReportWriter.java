package com.github.aiverifier.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerificationReport;
import com.github.aiverifier.core.service.ReportWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DefaultReportWriter implements ReportWriter {
    private final ObjectMapper jsonMapper;

    public DefaultReportWriter() {
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.jsonMapper.findAndRegisterModules();
    }

    @Override
    public void writeMarkdown(VerificationReport report, Path outputPath) {
        log.info("Writing markdown report to: {}", outputPath);
        StringBuilder sb = new StringBuilder();

        sb.append("# Verification Report: ").append(report.getTaskId()).append("\n\n");
        sb.append("## Status\n\n").append(report.getStatus()).append("\n\n");
        sb.append("## Generated Scenario\n\n").append(report.getGeneratedFeaturePath()).append("\n\n");

        if (!report.getExecutedChecks().isEmpty()) {
            sb.append("## Executed Checks\n\n");
            report.getExecutedChecks().forEach(c -> sb.append("- ").append(c).append("\n"));
            sb.append("\n");
        }

        if (!report.getPassed().isEmpty()) {
            sb.append("## Passed\n\n");
            report.getPassed().forEach(p -> sb.append("- ").append(p).append("\n"));
            sb.append("\n");
        }

        if (!report.getFailed().isEmpty()) {
            sb.append("## Failed\n\n");
            report.getFailed().forEach(f -> sb.append("- ").append(f).append("\n"));
            sb.append("\n");
        }

        if (!report.getNotVerified().isEmpty()) {
            sb.append("## Not Verified\n\n");
            report.getNotVerified().forEach(n -> sb.append("- ").append(n).append("\n"));
            sb.append("\n");
        }

        appendSection(sb, "Affected Endpoints", report.getAffectedEndpoints());
        appendSection(sb, "Coverage", report.getCoverage());
        appendSection(sb, "DB Checks", report.getDbChecks());
        appendSection(sb, "Async Checks", report.getAsyncChecks());
        appendSection(sb, "Not Verified Reasons", report.getNotVerifiedReasons());

        if (report.getErrorMessage() != null) {
            sb.append("## Error\n\n").append(report.getErrorMessage()).append("\n");
        }

        writeFile(outputPath, sb.toString());
    }

    @Override
    public void writeJson(VerificationReport report, Path outputPath) {
        log.info("Writing JSON report to: {}", outputPath);
        try {
            String json = jsonMapper.writeValueAsString(report);
            writeFile(outputPath, json);
        } catch (IOException e) {
            throw new VerifierException("Failed to serialize report to JSON: " + e.getMessage(), 6, e);
        }
    }

    private void writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new VerifierException("Failed to write report: " + e.getMessage(), 6, e);
        }
    }

    private void appendSection(StringBuilder sb, String title, java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sb.append("## ").append(title).append("\n\n");
        values.forEach(value -> sb.append("- ").append(value).append("\n"));
        sb.append("\n");
    }
}
