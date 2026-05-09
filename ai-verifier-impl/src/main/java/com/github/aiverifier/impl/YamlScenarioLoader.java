package com.github.aiverifier.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.ScenarioRequest;
import com.github.aiverifier.core.service.ScenarioLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class YamlScenarioLoader implements ScenarioLoader {
    private final ObjectMapper yamlMapper;

    public YamlScenarioLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ScenarioRequest load(Path scenarioPath) {
        log.info("Loading scenario from: {}", scenarioPath);

        if (!Files.exists(scenarioPath)) {
            throw new VerifierException("Scenario file not found: " + scenarioPath, 2);
        }

        try {
            ScenarioRequest scenario = yamlMapper.readValue(scenarioPath.toFile(), ScenarioRequest.class);
            if (scenario.getTask() == null || scenario.getTask().getId() == null) {
                throw new VerifierException("task.id is required in scenario", 2);
            }
            log.info("Scenario loaded: {}", scenario.getTask().getId());
            return scenario;
        } catch (IOException e) {
            throw new VerifierException("Failed to parse scenario: " + e.getMessage(), 2, e);
        }
    }
}
