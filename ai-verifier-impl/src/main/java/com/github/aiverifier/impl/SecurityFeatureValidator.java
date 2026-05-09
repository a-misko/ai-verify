package com.github.aiverifier.impl;

import com.github.aiverifier.core.model.ValidationResult;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.FeatureValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SecurityFeatureValidator implements FeatureValidator {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://([^/\\s'\"]+)");
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public ValidationResult validate(String featureContent, VerifierConfig.SecurityConfig security) {
        log.info("Validating generated feature file");
        List<String> violations = new ArrayList<>();

        if (featureContent == null || featureContent.isBlank()) {
            violations.add("Feature file is empty");
            return ValidationResult.failure(violations);
        }

        if (!featureContent.contains("Feature:")) {
            violations.add("Missing 'Feature:' declaration");
        }

        checkForbiddenMethods(featureContent, security, violations);
        checkForbiddenHosts(featureContent, security, violations);
        checkShellExecution(featureContent, security, violations);
        checkSqlReadonly(featureContent, security, violations);

        if (violations.isEmpty()) {
            log.info("Feature validation passed");
            return ValidationResult.success();
        } else {
            log.warn("Feature validation failed with {} violations", violations.size());
            return ValidationResult.failure(violations);
        }
    }

    private void checkForbiddenMethods(String content, VerifierConfig.SecurityConfig security, List<String> violations) {
        if (security == null || security.getForbiddenMethods() == null) return;

        for (String method : security.getForbiddenMethods()) {
            Pattern p = Pattern.compile("\\bmethod\\s+" + method.toLowerCase() + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(content).find()) {
                violations.add("Forbidden HTTP method used: " + method);
            }
        }
    }

    private void checkForbiddenHosts(String content, VerifierConfig.SecurityConfig security, List<String> violations) {
        if (security == null || security.getAllowedHosts() == null) return;

        Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            String host = matcher.group(1).split(":")[0];
            if (!security.getAllowedHosts().contains(host)) {
                violations.add("URL with disallowed host: " + host);
            }
        }
    }

    private void checkShellExecution(String content, VerifierConfig.SecurityConfig security, List<String> violations) {
        if (security != null && !security.isAllowShellExecutionInGeneratedScenario()) {
            if (content.contains("karate.exec(") || content.contains("karate.fork(")) {
                violations.add("Shell execution detected in generated feature");
            }
        }
    }

    private void checkSqlReadonly(String content, VerifierConfig.SecurityConfig security, List<String> violations) {
        if (security != null && security.isSqlReadonlyOnly()) {
            Matcher matcher = SQL_PATTERN.matcher(content);
            while (matcher.find()) {
                violations.add("Non-readonly SQL detected: " + matcher.group(1));
            }
        }
    }
}
