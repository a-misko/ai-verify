package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.service.FeatureExtractor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DefaultFeatureExtractor implements FeatureExtractor {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:gherkin|feature)?\\s*\\n(.*?)```", Pattern.DOTALL);

    @Override
    public String extractFeature(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            throw new VerifierException("Empty AI response", 4);
        }

        // Try to extract from code block first
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(aiResponse);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            log.info("Extracted feature from code block ({} chars)", content.length());
            return content;
        }

        // If the response itself starts with "Feature:", use it as-is
        String trimmed = aiResponse.trim();
        if (trimmed.startsWith("Feature:")) {
            log.info("Response is a raw feature file ({} chars)", trimmed.length());
            return trimmed;
        }

        throw new VerifierException("Could not extract Karate feature from AI response", 4);
    }
}
