package com.github.aiverifier.core.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationResult {

    private final boolean valid;
    private final List<String> violations;

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(List<String> violations) {
        return new ValidationResult(false, new ArrayList<>(violations));
    }
}
