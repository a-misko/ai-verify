package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.service.AiProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClaudeCodeAiProvider implements AiProvider {
    private final String claudeCommand;

    public ClaudeCodeAiProvider(String claudeCommand) {
        this.claudeCommand = claudeCommand != null ? claudeCommand : "claude";
    }

    @Override
    public String generate(String prompt, String projectPath, int timeoutSeconds) {
        log.info("Calling Claude Code (timeout: {}s)", timeoutSeconds);

        try {
            ProcessBuilder pb = new ProcessBuilder(claudeCommand, "-p", prompt);
            pb.directory(Path.of(projectPath).toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new VerifierException("Claude Code timed out after " + timeoutSeconds + "s", 4);
            }

            if (process.exitValue() != 0) {
                log.error("Claude Code stderr: {}", stderr);
                throw new VerifierException("Claude Code failed (exit " + process.exitValue() + "): " + stderr, 4);
            }

            log.info("Claude Code response received ({} chars)", stdout.length());
            return stdout;
        } catch (VerifierException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new VerifierException("Failed to run Claude Code: " + e.getMessage(), 4, e);
        }
    }
}
