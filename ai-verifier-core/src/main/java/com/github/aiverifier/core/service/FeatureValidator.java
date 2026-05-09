package com.github.aiverifier.core.service;

import com.github.aiverifier.core.model.ValidationResult;
import com.github.aiverifier.core.model.VerifierConfig;

public interface FeatureValidator {

    ValidationResult validate(String featureContent, VerifierConfig.SecurityConfig security);
}
