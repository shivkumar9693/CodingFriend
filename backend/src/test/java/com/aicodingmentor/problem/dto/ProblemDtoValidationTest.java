package com.aicodingmentor.problem.dto;

import com.aicodingmentor.problem.domain.ProblemDifficulty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProblemDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid CreateProblemRequest passes validation")
    void validCreateProblemRequest_Passes() {
        CreateProblemRequest request = new CreateProblemRequest(
                "Valid Problem",
                "valid-problem",
                "Description here",
                ProblemDifficulty.EASY,
                "Constraints",
                "Input",
                "Output",
                "Examples",
                "Starter code",
                "JAVA",
                Set.of("ARRAY"),
                List.of(new CreateTestCaseRequest("in", "out", false, 1, "exp"))
        );

        Set<ConstraintViolation<CreateProblemRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Blank title in CreateProblemRequest fails validation")
    void blankTitle_Fails() {
        CreateProblemRequest request = new CreateProblemRequest(
                "  ",
                "slug",
                "Description",
                ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", null, null
        );

        Set<ConstraintViolation<CreateProblemRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("Null difficulty in CreateProblemRequest fails validation")
    void nullDifficulty_Fails() {
        CreateProblemRequest request = new CreateProblemRequest(
                "Title Here",
                "slug",
                "Description",
                null,
                null, null, null, null, null, "JAVA", null, null
        );

        Set<ConstraintViolation<CreateProblemRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("difficulty")));
    }

    @Test
    @DisplayName("Blank expectedOutput in CreateTestCaseRequest fails validation")
    void blankExpectedOutput_TestCase_Fails() {
        CreateTestCaseRequest request = new CreateTestCaseRequest("input data", "   ", false, 1, null);

        Set<ConstraintViolation<CreateTestCaseRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("expectedOutput")));
    }
}
