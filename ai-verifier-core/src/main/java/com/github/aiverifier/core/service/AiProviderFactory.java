package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.VerifierConfig;

public interface AiProviderFactory {

    AiProvider create(VerifierConfig config);

    int resolveTimeoutSeconds(VerifierConfig config);
}
