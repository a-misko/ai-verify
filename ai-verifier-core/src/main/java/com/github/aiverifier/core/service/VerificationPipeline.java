package com.github.aiverifier.core.service;

import java.nio.file.Path;

public interface VerificationPipeline {

    int execute(Path configPath, Path scenarioPath);
}
