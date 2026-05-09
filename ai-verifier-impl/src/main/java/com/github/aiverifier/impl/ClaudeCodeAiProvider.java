package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.service.AiProvider;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class ClaudeCodeAiProvider implements AiProvider {
    private final String claudeCommand;

    public ClaudeCodeAiProvider(String claudeCommand) {
        this.claudeCommand = claudeCommand != null ? claudeCommand : "claude";
    }

    @Override
    public String generate(String prompt, String projectPath, int timeoutSeconds) {
        log.info("Calling Claude Code (timeout: {}s)", timeoutSeconds);

        CliProcessRunner.CliResult result = CliProcessRunner.run(
                buildCommand(),
                Path.of(projectPath),
                prompt,
                timeoutSeconds,
                "Claude Code");

        if (result.exitCode() != 0) {
            log.error("Claude Code stderr: {}", result.stderr());
            throw new VerifierException("Claude Code failed (exit " + result.exitCode() + "): " + result.stderr(), 4);
        }

        log.info("Claude Code response received ({} chars)", result.stdout().length());
        return result.stdout();
    }

    private List<String> buildCommand() {
        return List.of(
                claudeCommand,
                "-p",
                "--input-format",
                "text",
                "--output-format",
                "text",
                "--tools",
                "",
                "--no-session-persistence");
    }
}
