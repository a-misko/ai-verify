package com.github.aiverifier.launcher;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new VerifyCommand()).execute(args);
        System.exit(exitCode);
    }
}
