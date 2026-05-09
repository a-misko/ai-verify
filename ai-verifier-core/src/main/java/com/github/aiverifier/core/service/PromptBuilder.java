package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.model.ProjectInventory;

public interface PromptBuilder {

    String buildPrompt(VerifierConfig config, ScenarioRequest scenario, String gitDiff, ProjectInventory inventory);
}
