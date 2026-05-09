package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.model.VerificationReport;

import java.nio.file.Path;

public interface KarateRunner {

    VerificationReport run(Path featurePath, VerifierConfig config);
}
