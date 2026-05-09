package com.github.aiverifier.core.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class VerificationReport {

    public enum Status { PASSED, FAILED, ERROR }

    private String taskId;
    private Status status;
    private String generatedFeaturePath;
    private Instant timestamp;
    private List<String> executedChecks = new ArrayList<>();
    private List<String> passed = new ArrayList<>();
    private List<String> failed = new ArrayList<>();
    private List<String> notVerified = new ArrayList<>();
    private List<String> affectedEndpoints = new ArrayList<>();
    private List<String> coverage = new ArrayList<>();
    private List<String> dbChecks = new ArrayList<>();
    private List<String> asyncChecks = new ArrayList<>();
    private List<String> notVerifiedReasons = new ArrayList<>();
    private String errorMessage;
}
