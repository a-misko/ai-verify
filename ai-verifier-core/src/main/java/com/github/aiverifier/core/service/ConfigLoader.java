package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.VerifierConfig;

import java.nio.file.Path;

public interface ConfigLoader {

    VerifierConfig load(Path configPath);
}
