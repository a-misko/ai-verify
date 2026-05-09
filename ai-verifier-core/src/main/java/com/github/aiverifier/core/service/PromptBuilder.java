package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.model.VerifierConfig;

public interface PromptBuilder {

    String buildPrompt(VerifierConfig config, ScenarioRequest scenario, String gitDiff);
}
