package com.aicodingmentor.auth.dto;

import com.aicodingmentor.user.domain.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid RegisterRequest passes bean validation")
    void testValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest("learner@example.com", "Password123!", "Learner One", UserRole.USER);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("RegisterRequest with invalid email, short password, or blank name fails validation")
    void testInvalidRegisterRequest() {
        RegisterRequest invalidRequest = new RegisterRequest("not-an-email", "short", "   ", null);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(invalidRequest);

        assertEquals(3, violations.size());
    }

    @Test
    @DisplayName("Valid LoginRequest passes validation")
    void testValidLoginRequest() {
        LoginRequest request = new LoginRequest("learner@example.com", "Password123!");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("LoginRequest with blank fields fails validation")
    void testInvalidLoginRequest() {
        LoginRequest request = new LoginRequest("", "");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("RefreshTokenRequest with blank token fails validation")
    void testInvalidRefreshTokenRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest("   ");
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
