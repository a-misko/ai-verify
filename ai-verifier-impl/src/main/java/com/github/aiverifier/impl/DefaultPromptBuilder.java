package com.github.aiverifier.impl;

import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.PromptBuilder;

import java.util.stream.Collectors;

public class DefaultPromptBuilder implements PromptBuilder {

    @Override
    public String buildPrompt(VerifierConfig config, ScenarioRequest scenario, String gitDiff) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a backend verification engineer.\n\n");
        sb.append("Goal:\n");
        sb.append("Generate exactly one valid Karate .feature file for runtime verification.\n\n");

        sb.append("## Task\n");
        sb.append("ID: ").append(scenario.getTask().getId()).append("\n");
        sb.append("Title: ").append(scenario.getTask().getTitle()).append("\n");
        sb.append("Description: ").append(scenario.getTask().getDescription()).append("\n\n");

        if (scenario.getVerificationRequest() != null && scenario.getVerificationRequest().getFocus() != null) {
            sb.append("## Verification Focus\n");
            scenario.getVerificationRequest().getFocus().forEach(f -> sb.append("- ").append(f).append("\n"));
            sb.append("\n");
        }

        if (scenario.getExpectedBehavior() != null) {
            sb.append("## Expected Behavior\n");
            scenario.getExpectedBehavior().forEach(b -> sb.append("- ").append(b).append("\n"));
            sb.append("\n");
        }

        sb.append("## Runtime Config\n");
        sb.append("baseUrl: ").append(config.getApp().getBaseUrl()).append("\n");
        if (config.getAuth() != null && config.getAuth().getToken() != null) {
            sb.append("auth: bearer token provided as variable 'bearerToken'\n");
        }
        if (config.getTestData() != null) {
            sb.append("testData: ").append(config.getTestData().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "))).append("\n");
        }
        sb.append("\n");

        sb.append("## Git Diff\n```\n").append(gitDiff).append("\n```\n\n");

        sb.append("## Rules\n");
        sb.append("1. Do not modify production files.\n");
        sb.append("2. Do not run application tests.\n");
        sb.append("3. Do not execute HTTP requests.\n");
        sb.append("4. Do not execute SQL.\n");
        sb.append("5. Only generate a Karate .feature file.\n");
        sb.append("6. Use only provided baseUrl variable.\n");
        sb.append("7. Use only provided testData.\n");

        if (config.getSecurity() != null && config.getSecurity().getForbiddenMethods() != null) {
            String forbidden = String.join(", ", config.getSecurity().getForbiddenMethods());
            sb.append("8. Do not use ").append(forbidden).append(".\n");
        }

        sb.append("9. Do not use external URLs.\n");
        sb.append("10. If required test data is missing, generate a scenario that fails with a clear readable message.\n");
        sb.append("11. Return only the feature file content. No explanation.\n");

        return sb.toString();
    }
}
