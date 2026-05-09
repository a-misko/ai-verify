package com.github.aiverifier.impl;

import com.github.aiverifier.core.exception.VerifierException;
import com.github.aiverifier.core.model.VerifierConfig;
import com.github.aiverifier.core.service.EnvironmentChecker;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class HttpEnvironmentChecker implements EnvironmentChecker {
    private final HttpClient httpClient;

    public HttpEnvironmentChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void check(VerifierConfig config) {
        checkHealth(config.getApp());
    }

    private void checkHealth(VerifierConfig.AppConfig app) {
        String healthUrl = app.getHealthUrl() != null ? app.getHealthUrl() : app.getBaseUrl();
        log.info("Checking application health: {}", healthUrl);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new VerifierException(
                        "Health check failed with status " + response.statusCode() + ": " + healthUrl, 3);
            }

            log.info("Application health check passed (status {})", response.statusCode());
        } catch (VerifierException e) {
            throw e;
        } catch (Exception e) {
            throw new VerifierException("Application is not reachable: " + healthUrl + " (" + e.getMessage() + ")", 3, e);
        }
    }
}
