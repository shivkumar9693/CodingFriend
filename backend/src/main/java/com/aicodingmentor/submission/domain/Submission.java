package com.aicodingmentor.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "language", nullable = false, length = 50)
    private String language = "JAVA";

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SubmissionStatus status = SubmissionStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", length = 50)
    private com.aicodingmentor.execution.model.EvaluationVerdict verdict;

    @Column(name = "tests_passed", nullable = false)
    private int testsPassed = 0;

    @Column(name = "total_tests", nullable = false)
    private int totalTests = 0;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMs = 0L;

    @Column(name = "evaluation_details", columnDefinition = "TEXT")
    private String evaluationDetails;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Submission() {
        // Required by JPA
    }

    public Submission(UUID userId, UUID problemId, String sourceCode, String language) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.problemId = Objects.requireNonNull(problemId, "problemId must not be null");
        this.sourceCode = Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        if (language != null && !language.isBlank()) {
            this.language = language.toUpperCase();
        }
        this.status = SubmissionStatus.QUEUED;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SubmissionStatus.QUEUED;
        }
        if (this.language == null || this.language.isBlank()) {
            this.language = "JAVA";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public String getLanguage() {
        return language;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public com.aicodingmentor.execution.model.EvaluationVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(com.aicodingmentor.execution.model.EvaluationVerdict verdict) {
        this.verdict = verdict;
    }

    public int getTestsPassed() {
        return testsPassed;
    }

    public void setTestsPassed(int testsPassed) {
        this.testsPassed = testsPassed;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getEvaluationDetails() {
        return evaluationDetails;
    }

    public void setEvaluationDetails(String evaluationDetails) {
        this.evaluationDetails = evaluationDetails;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markRunning() {
        this.status = SubmissionStatus.RUNNING;
    }

    public void markEvaluating() {
        this.status = SubmissionStatus.EVALUATING;
    }

    public void markCompleted(com.aicodingmentor.execution.model.EvaluationVerdict verdict, int passed, int total, long timeMs, String details) {
        this.status = SubmissionStatus.COMPLETED;
        this.verdict = verdict;
        this.testsPassed = passed;
        this.totalTests = total;
        this.executionTimeMs = timeMs;
        this.evaluationDetails = details;
    }

    public void markCompleted(com.aicodingmentor.execution.model.EvaluationVerdict verdict, int passed, int total, long timeMs, String details, String errorMessage) {
        this.status = SubmissionStatus.COMPLETED;
        this.verdict = verdict;
        this.testsPassed = passed;
        this.totalTests = total;
        this.executionTimeMs = timeMs;
        this.evaluationDetails = details;
        this.errorMessage = errorMessage;
    }

    public void markCompleted() {
        this.status = SubmissionStatus.COMPLETED;
    }

    public void markFailed(String error) {
        this.status = SubmissionStatus.FAILED;
        this.errorMessage = error;
    }
}
