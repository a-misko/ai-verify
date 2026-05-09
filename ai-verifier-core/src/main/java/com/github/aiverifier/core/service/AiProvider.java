package com.github.aiverifier.core.service;

public interface AiProvider {

    String generate(String prompt, String projectPath, int timeoutSeconds);
}
