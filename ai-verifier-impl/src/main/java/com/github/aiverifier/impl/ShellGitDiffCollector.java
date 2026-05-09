package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.GitDiffCollector;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ShellGitDiffCollector implements GitDiffCollector {

    private static final int DIFF_TIMEOUT_SECONDS = 30;

    @Override
    public String collectDiff(VerifierConfig config) {
        String diffCommand = "git diff main...HEAD";
        if (config.getGit() != null && config.getGit().getDiffCommand() != null) {
            diffCommand = config.getGit().getDiffCommand();
        }

        Path projectDir = Path.of(config.getProject().getPath());
        log.info("Collecting git diff in: {} using: {}", projectDir, diffCommand);

        CliProcessRunner.CliResult result = CliProcessRunner.run(
                buildCommand(diffCommand),
                projectDir,
                null,
                DIFF_TIMEOUT_SECONDS,
                "git diff");

        if (result.exitCode() != 0) {
            throw new VerifierException("Git diff failed: " + result.stderr() + result.stdout(), 3);
        }

        log.info("Git diff collected ({} chars)", result.stdout().length());
        return result.stdout();
    }

    private List<String> buildCommand(String diffCommand) {
        List<String> parts = splitCommand(diffCommand);
        if (!parts.isEmpty() && "git".equals(parts.get(0))) {
            return parts;
        }

        if (isWindows()) {
            return List.of("cmd.exe", "/c", diffCommand);
        }
        return List.of("sh", "-c", diffCommand);
    }

    private List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(ch) && !inSingleQuotes && !inDoubleQuotes) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
