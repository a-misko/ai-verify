package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.ScenarioRequest;

import java.nio.file.Path;

public interface ScenarioLoader {

    ScenarioRequest load(Path scenarioPath);
}
