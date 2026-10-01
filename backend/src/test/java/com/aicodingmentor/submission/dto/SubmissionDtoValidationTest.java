package com.aicodingmentor.submission.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid submission request passes validation")
    void validSubmissionRequest_Passes() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                UUID.randomUUID(),
                "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; } }",
                "JAVA"
        );

        Set<ConstraintViolation<CreateSubmissionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no validation violations for valid request");
        assertEquals("JAVA", request.language());
    }

    @Test
    @DisplayName("Submission request with null language defaults to JAVA")
    void nullLanguage_DefaultsToJava() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                UUID.randomUUID(),
                "class Solution {}",
                null
        );

        Set<ConstraintViolation<CreateSubmissionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
        assertEquals("JAVA", request.language());
    }

    @Test
    @DisplayName("Submission request with null problemId fails validation")
    void nullProblemId_Fails() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null,
                "class Solution {}",
                "JAVA"
        );

        Set<ConstraintViolation<CreateSubmissionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("problemId")));
    }

    @Test
    @DisplayName("Submission request with blank source code fails validation")
    void blankSourceCode_Fails() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                UUID.randomUUID(),
                "   \n  \t ",
                "JAVA"
        );

        Set<ConstraintViolation<CreateSubmissionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sourceCode")));
    }

    @Test
    @DisplayName("Submission request with unsupported language fails validation")
    void unsupportedLanguage_Fails() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                UUID.randomUUID(),
                "print('Hello')",
                "PYTHON"
        );

        Set<ConstraintViolation<CreateSubmissionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("language")));
    }
}
