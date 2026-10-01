package com.aicodingmentor.mentor.dto;

import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.problem.domain.Problem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlled, immutable context DTO for the Socratic AI Prompt Builder.
 * Encapsulates problem metadata, learner source code, diagnostic mistakes,
 * test failure summaries, execution metrics, previous hint history, and mistake trends.
 */
@Schema(description = "Structured context provided to AI Prompt Builder")
public record AiPromptContext(
        Problem problem,
        String sourceCode,
        String programmingLanguage,
        HintLevel hintLevel,
        String mistakeType,
        String compilerOrRuntimeError,
        String failingTestSummary,
        ExecutionMetricsContext executionMetrics,
        List<String> previousHints,
        List<String> learnerMistakeHistory,
        String userQuestion
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Problem problem;
        private String sourceCode = "";
        private String programmingLanguage = "JAVA";
        private HintLevel hintLevel = HintLevel.LEVEL_1_NUDGE;
        private String mistakeType;
        private String compilerOrRuntimeError;
        private String failingTestSummary;
        private ExecutionMetricsContext executionMetrics;
        private List<String> previousHints = new ArrayList<>();
        private List<String> learnerMistakeHistory = new ArrayList<>();
        private String userQuestion;

        public Builder problem(Problem problem) {
            this.problem = problem;
            return this;
        }

        public Builder sourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
            return this;
        }

        public Builder programmingLanguage(String programmingLanguage) {
            this.programmingLanguage = programmingLanguage;
            return this;
        }

        public Builder hintLevel(HintLevel hintLevel) {
            this.hintLevel = hintLevel;
            return this;
        }

        public Builder mistakeType(String mistakeType) {
            this.mistakeType = mistakeType;
            return this;
        }

        public Builder compilerOrRuntimeError(String compilerOrRuntimeError) {
            this.compilerOrRuntimeError = compilerOrRuntimeError;
            return this;
        }

        public Builder failingTestSummary(String failingTestSummary) {
            this.failingTestSummary = failingTestSummary;
            return this;
        }

        public Builder executionMetrics(ExecutionMetricsContext executionMetrics) {
            this.executionMetrics = executionMetrics;
            return this;
        }

        public Builder previousHints(List<String> previousHints) {
            this.previousHints = previousHints != null ? new ArrayList<>(previousHints) : new ArrayList<>();
            return this;
        }

        public Builder addPreviousHint(String hint) {
            if (this.previousHints == null) {
                this.previousHints = new ArrayList<>();
            }
            if (hint != null && !hint.isBlank()) {
                this.previousHints.add(hint);
            }
            return this;
        }

        public Builder learnerMistakeHistory(List<String> learnerMistakeHistory) {
            this.learnerMistakeHistory = learnerMistakeHistory != null ? new ArrayList<>(learnerMistakeHistory) : new ArrayList<>();
            return this;
        }

        public Builder addLearnerMistake(String mistake) {
            if (this.learnerMistakeHistory == null) {
                this.learnerMistakeHistory = new ArrayList<>();
            }
            if (mistake != null && !mistake.isBlank()) {
                this.learnerMistakeHistory.add(mistake);
            }
            return this;
        }

        public Builder userQuestion(String userQuestion) {
            this.userQuestion = userQuestion;
            return this;
        }

        public AiPromptContext build() {
            return new AiPromptContext(
                    problem,
                    sourceCode != null ? sourceCode : "",
                    programmingLanguage != null ? programmingLanguage : "JAVA",
                    hintLevel != null ? hintLevel : HintLevel.LEVEL_1_NUDGE,
                    mistakeType,
                    compilerOrRuntimeError,
                    failingTestSummary,
                    executionMetrics,
                    previousHints != null ? List.copyOf(previousHints) : List.of(),
                    learnerMistakeHistory != null ? List.copyOf(learnerMistakeHistory) : List.of(),
                    userQuestion
            );
        }
    }
}
