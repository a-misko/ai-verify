package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.GitDiffCollector;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShellGitDiffCollector implements GitDiffCollector {
    @Override
    public String collectDiff(VerifierConfig config) {
        String diffCommand = "git diff main...HEAD";
        if (config.getGit() != null && config.getGit().getDiffCommand() != null) {
            diffCommand = config.getGit().getDiffCommand();
        }

        Path projectDir = Path.of(config.getProject().getPath());
        log.info("Collecting git diff in: {} using: {}", projectDir, diffCommand);

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", diffCommand);
            pb.directory(projectDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new VerifierException("Git diff timed out", 3);
            }

            if (process.exitValue() != 0) {
                throw new VerifierException("Git diff failed: " + output, 3);
            }

            log.info("Git diff collected ({} chars)", output.length());
            return output;
        } catch (VerifierException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new VerifierException("Failed to run git diff: " + e.getMessage(), 3, e);
        }
    }
}
