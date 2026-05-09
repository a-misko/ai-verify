package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.VerifierConfig;

public interface GitDiffCollector {

    String collectDiff(VerifierConfig config);
}
