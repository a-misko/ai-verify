package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.PromptBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DefaultPromptBuilder implements PromptBuilder {

    private static final String DEFAULT_PROMPT_RESOURCE = "prompts/default-ai-prompt.md";

    @Override
    public String buildPrompt(VerifierConfig config, ScenarioRequest scenario, String gitDiff) {
        StringBuilder sb = new StringBuilder();

        sb.append(loadDefaultPrompt()).append("\n\n");
        sb.append("## Current Scenario Context\n\n");

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
        if (config.getTestData() != null) {
            sb.append("testData: ").append(config.getTestData().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "))).append("\n");
        }
        if (config.getDatabase() != null && config.getDatabase().isEnabled()) {
            sb.append("database: available as variable 'database' with fields enabled, type, jdbcUrl, username, password, readonly\n");
            sb.append("database.type: ").append(config.getDatabase().getType()).append("\n");
            sb.append("database.readonly: ").append(config.getDatabase().isReadonly()).append("\n");
        }
        sb.append("\n");

        sb.append("## Git Diff\n```\n").append(gitDiff).append("\n```\n\n");

        if (config.getSecurity() != null && config.getSecurity().getForbiddenMethods() != null) {
            String forbidden = String.join(", ", config.getSecurity().getForbiddenMethods());
            sb.append("## Configured Security Restrictions\n");
            sb.append("Do not use these HTTP methods: ").append(forbidden).append(".\n\n");
        }

        return sb.toString();
    }

    private String loadDefaultPrompt() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_PROMPT_RESOURCE)) {
            if (inputStream == null) {
                throw new VerifierException("Default AI prompt resource not found: " + DEFAULT_PROMPT_RESOURCE, 4);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new VerifierException("Failed to load default AI prompt: " + e.getMessage(), 4, e);
        }
    }
}
