package com.github.aiverifier.core.model;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioRequest {

    private TaskInfo task;
    private VerificationRequest verificationRequest;
    private List<String> expectedBehavior;
    private Constraints constraints;

    @Data
    public static class TaskInfo {
        private String id;
        private String title;
        private String description;
    }

    @Data
    public static class VerificationRequest {
        private List<String> focus;
    }

    @Data
    public static class Constraints {
        private boolean destructiveOperationsAllowed;
        private boolean createNewEntitiesAllowed;
        private boolean useExistingTestData;
    }
}
