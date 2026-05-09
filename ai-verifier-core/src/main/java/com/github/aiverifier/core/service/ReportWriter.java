package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.VerificationReport;

import java.nio.file.Path;

public interface ReportWriter {

    void writeMarkdown(VerificationReport report, Path outputPath);

    void writeJson(VerificationReport report, Path outputPath);
}
