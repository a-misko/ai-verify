package com.github.aiverifier.core.exception;

public class VerifierException extends RuntimeException {

    private final int exitCode;

    public VerifierException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public VerifierException(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() { return exitCode; }
}
