package com.github.aiverifier.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class YamlConfigLoader implements ConfigLoader {
    private final ObjectMapper yamlMapper;

    public YamlConfigLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public VerifierConfig load(Path configPath) {
        log.info("Loading config from: {}", configPath);

        if (!Files.exists(configPath)) {
            throw new VerifierException("Config file not found: " + configPath, 2);
        }

        try {
            VerifierConfig config = yamlMapper.readValue(configPath.toFile(), VerifierConfig.class);
            validate(config);
            log.info("Config loaded successfully");
            return config;
        } catch (IOException e) {
            throw new VerifierException("Failed to parse config: " + e.getMessage(), 2, e);
        }
    }

    private void validate(VerifierConfig config) {
        if (config.getProject() == null || config.getProject().getPath() == null) {
            throw new VerifierException("project.path is required in config", 2);
        }
        if (!Files.isDirectory(Path.of(config.getProject().getPath()))) {
            throw new VerifierException("project.path does not exist: " + config.getProject().getPath(), 2);
        }
        if (config.getApp() == null || config.getApp().getBaseUrl() == null) {
            throw new VerifierException("app.baseUrl is required in config", 2);
        }
    }
}
