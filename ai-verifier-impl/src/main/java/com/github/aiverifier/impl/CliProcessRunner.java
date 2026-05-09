package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class CliProcessRunner {

    private CliProcessRunner() {
    }

    static CliResult run(List<String> command, Path directory, String stdin, int timeoutSeconds, String processName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(directory.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());

            if (stdin != null) {
                process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
            }
            process.getOutputStream().close();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new VerifierException(processName + " timed out after " + timeoutSeconds + "s", 4);
            }

            return new CliResult(process.exitValue(), stdout.get(), stderr.get());
        } catch (VerifierException e) {
            throw e;
        } catch (IOException e) {
            throw new VerifierException("Failed to run " + processName + ": " + e.getMessage(), 4, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VerifierException(processName + " was interrupted", 4, e);
        } catch (ExecutionException e) {
            throw new VerifierException("Failed to read " + processName + " output: " + e.getMessage(), 4, e);
        }
    }

    private static CompletableFuture<String> readAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    record CliResult(int exitCode, String stdout, String stderr) {
    }
}
