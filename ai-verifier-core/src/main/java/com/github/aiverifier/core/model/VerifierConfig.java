package com.github.aiverifier.core.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VerifierConfig {

    private ProjectConfig project;
    private GitConfig git;
    private AppConfig app;
    private AuthConfig auth;
    private DatabaseConfig database;
    private Map<String, Object> testData;
    private AiConfig ai;
    private ClaudeCodeConfig claudeCode;
    private CodexCodeConfig codexCode;
    private KarateConfig karate;
    private SecurityConfig security;

    @Data
    public static class ProjectConfig {
        private String path;
        private String language;
        private String framework;
    }

    @Data
    public static class GitConfig {
        private String baseBranch;
        private String currentBranch;
        private String diffCommand;
    }

    @Data
    public static class AppConfig {
        private String baseUrl;
        private String healthUrl;
    }

    @Data
    public static class AuthConfig {
        private String type;
        private String token;
    }

    @Data
    public static class DatabaseConfig {
        private boolean enabled;
        private String type;
        private String jdbcUrl;
        private String username;
        private String password;
        private boolean readonly;
    }

    @Data
    public static class AiConfig {
        private String provider;
    }

    @Data
    public static class ClaudeCodeConfig {
        private boolean enabled;
        private String command;
        private int timeoutSeconds;
    }

    @Data
    public static class CodexCodeConfig {
        private boolean enabled;
        private String command;
        private int timeoutSeconds;
        private String model;
        private String profile;
    }

    @Data
    public static class KarateConfig {
        private String outputDir;
        private String reportDir;
    }

    @Data
    public static class SecurityConfig {
        private List<String> allowedMethods;
        private List<String> forbiddenMethods;
        private List<String> allowedHosts;
        private boolean sqlReadonlyOnly;
        private boolean allowShellExecutionInGeneratedScenario;
    }
}
