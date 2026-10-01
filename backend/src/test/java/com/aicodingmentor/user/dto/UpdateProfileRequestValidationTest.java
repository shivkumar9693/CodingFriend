package com.aicodingmentor.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProfileRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid UpdateProfileRequest passes all validation rules")
    void validRequest_NoViolations() {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", "Passionate Java coder", "janedoe-dev");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Blank full name fails validation")
    void blankFullName_ValidationFails() {
        UpdateProfileRequest request = new UpdateProfileRequest("   ", "Bio text", "janedoe");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    @DisplayName("Full name shorter than 2 characters fails validation")
    void tooShortFullName_ValidationFails() {
        UpdateProfileRequest request = new UpdateProfileRequest("A", "Bio text", "janedoe");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    @DisplayName("Bio exceeding 500 characters fails validation")
    void bioExceedingLimit_ValidationFails() {
        String longBio = "a".repeat(501);
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", longBio, "janedoe");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bio")));
    }

    @Test
    @DisplayName("GitHub username with invalid special characters fails validation")
    void invalidGithubUsername_ValidationFails() {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", "Bio", "jane@invalid!name");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("githubUsername")));
    }
}
