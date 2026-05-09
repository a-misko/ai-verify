package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.service.AiProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CodexCodeAiProvider implements AiProvider {

    private final String codexCommand;
    private final String model;
    private final String profile;

    public CodexCodeAiProvider(String codexCommand, String model, String profile) {
        this.codexCommand = codexCommand != null ? codexCommand : defaultCodexCommand();
        this.model = model;
        this.profile = profile;
    }

    @Override
    public String generate(String prompt, String projectPath, int timeoutSeconds) {
        log.info("Calling Codex Code (timeout: {}s)", timeoutSeconds);

        try {
            ProcessBuilder pb = new ProcessBuilder(buildCommand(projectPath));
            pb.directory(Path.of(projectPath).toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            process.getOutputStream().write(prompt.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new VerifierException("Codex Code timed out after " + timeoutSeconds + "s", 4);
            }

            if (process.exitValue() != 0) {
                log.error("Codex Code stderr: {}", stderr);
                throw new VerifierException("Codex Code failed (exit " + process.exitValue() + "): " + stderr, 4);
            }

            log.info("Codex Code response received ({} chars)", stdout.length());
            return stdout;
        } catch (VerifierException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new VerifierException("Failed to run Codex Code: " + e.getMessage(), 4, e);
        }
    }

    private List<String> buildCommand(String projectPath) {
        List<String> command = new ArrayList<>();
        command.add(codexCommand);
        command.add("exec");
        command.add("--cd");
        command.add(projectPath);
        command.add("--skip-git-repo-check");

        if (model != null && !model.isBlank()) {
            command.add("--model");
            command.add(model);
        }
        if (profile != null && !profile.isBlank()) {
            command.add("--profile");
            command.add(profile);
        }

        command.add("-");
        return command;
    }

    private String defaultCodexCommand() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win") ? "codex.cmd" : "codex";
    }
}
