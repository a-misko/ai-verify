package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.AiProvider;
import com.github.aiverifier.core.service.AiProviderFactory;

import java.util.Locale;

public class DefaultAiProviderFactory implements AiProviderFactory {

    private static final String CLAUDE_CODE = "claude-code";
    private static final String CODEX_CODE = "codex-code";
    private static final int DEFAULT_TIMEOUT_SECONDS = 180;

    @Override
    public AiProvider create(VerifierConfig config) {
        String provider = resolveProvider(config);
        return switch (provider) {
            case CLAUDE_CODE -> new ClaudeCodeAiProvider(resolveClaudeCommand(config));
            case CODEX_CODE -> new CodexCodeAiProvider(
                    resolveCodexCommand(config),
                    resolveCodexModel(config),
                    resolveCodexProfile(config));
            default -> throw new VerifierException("Unsupported AI provider: " + provider, 2);
        };
    }

    @Override
    public int resolveTimeoutSeconds(VerifierConfig config) {
        String provider = resolveProvider(config);
        int timeout = switch (provider) {
            case CLAUDE_CODE -> config.getClaudeCode() != null ? config.getClaudeCode().getTimeoutSeconds() : 0;
            case CODEX_CODE -> config.getCodexCode() != null ? config.getCodexCode().getTimeoutSeconds() : 0;
            default -> throw new VerifierException("Unsupported AI provider: " + provider, 2);
        };
        return timeout > 0 ? timeout : DEFAULT_TIMEOUT_SECONDS;
    }

    private String resolveProvider(VerifierConfig config) {
        if (config.getAi() == null || config.getAi().getProvider() == null || config.getAi().getProvider().isBlank()) {
            return CLAUDE_CODE;
        }
        return config.getAi().getProvider().trim().toLowerCase(Locale.ROOT);
    }

    private String resolveClaudeCommand(VerifierConfig config) {
        if (config.getClaudeCode() == null || config.getClaudeCode().getCommand() == null) {
            return null;
        }
        return config.getClaudeCode().getCommand();
    }

    private String resolveCodexCommand(VerifierConfig config) {
        if (config.getCodexCode() == null || config.getCodexCode().getCommand() == null) {
            return null;
        }
        return config.getCodexCode().getCommand();
    }

    private String resolveCodexModel(VerifierConfig config) {
        if (config.getCodexCode() == null) {
            return null;
        }
        return config.getCodexCode().getModel();
    }

    private String resolveCodexProfile(VerifierConfig config) {
        if (config.getCodexCode() == null) {
            return null;
        }
        return config.getCodexCode().getProfile();
    }
}
