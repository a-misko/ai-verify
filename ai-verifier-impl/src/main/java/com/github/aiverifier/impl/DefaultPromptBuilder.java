package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.ProjectInventory;
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
    public String buildPrompt(VerifierConfig config, ScenarioRequest scenario, String gitDiff, ProjectInventory inventory) {
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
        sb.append("project.path: ").append(config.getProject().getPath()).append("\n");
        if (config.getProject().getLanguage() != null) {
            sb.append("project.language: ").append(config.getProject().getLanguage()).append("\n");
        }
        if (config.getProject().getFramework() != null) {
            sb.append("project.framework: ").append(config.getProject().getFramework()).append("\n");
        }
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
        appendAuthFlowConfig(sb, config);
        sb.append("\n");

        appendInventory(sb, inventory);

        sb.append("## Git Diff\n```\n").append(gitDiff).append("\n```\n\n");

        if (config.getSecurity() != null && config.getSecurity().getForbiddenMethods() != null) {
            String forbidden = String.join(", ", config.getSecurity().getForbiddenMethods());
            sb.append("## Configured Security Restrictions\n");
            sb.append("Do not use these HTTP methods: ").append(forbidden).append(".\n\n");
        }

        return sb.toString();
    }

    private void appendAuthFlowConfig(StringBuilder sb, VerifierConfig config) {
        if (config.getAuthFlow() == null) {
            sb.append("authFlow: not configured; infer auth flow from project source code\n");
            return;
        }

        sb.append("authFlow.registrationEndpoint: ").append(config.getAuthFlow().getRegistrationEndpoint()).append("\n");
        sb.append("authFlow.confirmationEndpoint: ").append(config.getAuthFlow().getConfirmationEndpoint()).append("\n");
        sb.append("authFlow.tokenEndpoint: ").append(config.getAuthFlow().getTokenEndpoint()).append("\n");
        sb.append("authFlow.notificationQuery: ").append(config.getAuthFlow().getNotificationQuery()).append("\n");
        sb.append("authFlow.confirmationLinkColumn: ").append(config.getAuthFlow().getConfirmationLinkColumn()).append("\n");
        sb.append("authFlow.confirmationTokenRegex: ").append(config.getAuthFlow().getConfirmationTokenRegex()).append("\n");
        sb.append("authFlow.accessTokenJsonPath: ").append(config.getAuthFlow().getAccessTokenJsonPath()).append("\n");
        sb.append("authFlow.refreshTokenJsonPath: ").append(config.getAuthFlow().getRefreshTokenJsonPath()).append("\n");
    }

    private void appendInventory(StringBuilder sb, ProjectInventory inventory) {
        sb.append("## Static Project Inventory\n");
        appendList(sb, "Affected Endpoints", inventory.getAffectedEndpoints());
        appendList(sb, "Relevant Source Files", inventory.getRelevantSourceFiles());
        appendList(sb, "Database Artifacts", inventory.getDatabaseArtifacts());
        appendList(sb, "Messaging Artifacts", inventory.getMessagingArtifacts());
        appendList(sb, "Auth Artifacts", inventory.getAuthArtifacts());
        appendList(sb, "Inventory Notes", inventory.getNotes());
        sb.append("\n");
    }

    private void appendList(StringBuilder sb, String title, java.util.List<String> values) {
        sb.append("### ").append(title).append("\n");
        if (values == null || values.isEmpty()) {
            sb.append("- none detected\n");
            return;
        }
        values.forEach(value -> sb.append("- ").append(value).append("\n"));
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
