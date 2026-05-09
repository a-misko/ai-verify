package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.service.AiProvider;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

        CliProcessRunner.CliResult result = CliProcessRunner.run(
                buildCommand(projectPath),
                Path.of(projectPath),
                prompt,
                timeoutSeconds,
                "Codex Code");

        if (result.exitCode() != 0) {
            log.error("Codex Code stderr: {}", result.stderr());
            throw new VerifierException("Codex Code failed (exit " + result.exitCode() + "): " + result.stderr(), 4);
        }

        log.info("Codex Code response received ({} chars)", result.stdout().length());
        return result.stdout();
    }

    private List<String> buildCommand(String projectPath) {
        List<String> command = new ArrayList<>();
        command.add(codexCommand);
        command.add("exec");
        command.add("--cd");
        command.add(projectPath);
        command.add("--skip-git-repo-check");
        command.add("--sandbox");
        command.add("read-only");
        command.add("--ephemeral");
        command.add("--color");
        command.add("never");

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
