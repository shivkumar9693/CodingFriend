package com.aicodingmentor.learning.domain;

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
import java.util.UUID;

/**
 * Entity representing an AI Post-Mortem & Revision Note for a learner.
 * Captures the exact input counter-example, actual code trace vs. invariant trace,
 * and tracks spaced repetition mastery.
 */
@Entity
@Table(name = "revision_notes")
public class RevisionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id")
    private UUID problemId;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "problem_title", nullable = false)
    private String problemTitle;

    @Column(name = "problem_slug")
    private String problemSlug;

    @Column(name = "language", nullable = false, length = 50)
    private String language = "JAVA";

    @Enumerated(EnumType.STRING)
    @Column(name = "mistake_category", nullable = false, length = 100)
    private MistakeCategory mistakeCategory = MistakeCategory.LOGIC_ERROR;

    @Column(name = "concept_summary", nullable = false, length = 500)
    private String conceptSummary;

    @Column(name = "failed_input", columnDefinition = "TEXT")
    private String failedInput;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "student_flow_trace", columnDefinition = "TEXT", nullable = false)
    private String studentFlowTrace;

    @Column(name = "correct_flow_trace", columnDefinition = "TEXT", nullable = false)
    private String correctFlowTrace;

    @Column(name = "key_takeaway", columnDefinition = "TEXT", nullable = false)
    private String keyTakeaway;

    @Column(name = "submitted_code", columnDefinition = "TEXT")
    private String submittedCode;

    @Column(name = "user_reflections", columnDefinition = "TEXT")
    private String userReflections;

    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_status", nullable = false, length = 50)
    private MasteryStatus masteryStatus = MasteryStatus.TO_REVISE;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean isBookmarked = false;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public RevisionNote() {
    }

    public RevisionNote(
            UUID userId,
            UUID problemId,
            UUID submissionId,
            String problemTitle,
            String problemSlug,
            String language,
            MistakeCategory mistakeCategory,
            String conceptSummary,
            String failedInput,
            String actualOutput,
            String expectedOutput,
            String studentFlowTrace,
            String correctFlowTrace,
            String keyTakeaway,
            String submittedCode,
            String userReflections
    ) {
        this.userId = userId;
        this.problemId = problemId;
        this.submissionId = submissionId;
        this.problemTitle = problemTitle;
        this.problemSlug = problemSlug;
        this.language = language != null ? language : "JAVA";
        this.mistakeCategory = mistakeCategory != null ? mistakeCategory : MistakeCategory.LOGIC_ERROR;
        this.conceptSummary = conceptSummary;
        this.failedInput = failedInput;
        this.actualOutput = actualOutput;
        this.expectedOutput = expectedOutput;
        this.studentFlowTrace = studentFlowTrace;
        this.correctFlowTrace = correctFlowTrace;
        this.keyTakeaway = keyTakeaway;
        this.submittedCode = submittedCode;
        this.userReflections = userReflections;
        this.masteryStatus = MasteryStatus.TO_REVISE;
        this.isBookmarked = false;
        this.reviewCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void incrementReview() {
        this.reviewCount++;
        this.lastReviewedAt = Instant.now();
        // Spaced repetition schedule: 1 day -> 3 days -> 7 days -> 14 days -> 30 days
        long daysToAdd = switch (reviewCount) {
            case 1 -> 1L;
            case 2 -> 3L;
            case 3 -> 7L;
            case 4 -> 14L;
            default -> 30L;
        };
        this.nextReviewAt = Instant.now().plusSeconds(daysToAdd * 86400L);
        if (this.reviewCount >= 3 && this.masteryStatus == MasteryStatus.TO_REVISE) {
            this.masteryStatus = MasteryStatus.REVISED;
        }
        if (this.reviewCount >= 5) {
            this.masteryStatus = MasteryStatus.MASTERED;
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public void setProblemId(UUID problemId) {
        this.problemId = problemId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public String getProblemSlug() {
        return problemSlug;
    }

    public void setProblemSlug(String problemSlug) {
        this.problemSlug = problemSlug;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public MistakeCategory getMistakeCategory() {
        return mistakeCategory;
    }

    public void setMistakeCategory(MistakeCategory mistakeCategory) {
        this.mistakeCategory = mistakeCategory;
    }

    public String getConceptSummary() {
        return conceptSummary;
    }

    public void setConceptSummary(String conceptSummary) {
        this.conceptSummary = conceptSummary;
    }

    public String getFailedInput() {
        return failedInput;
    }

    public void setFailedInput(String failedInput) {
        this.failedInput = failedInput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getStudentFlowTrace() {
        return studentFlowTrace;
    }

    public void setStudentFlowTrace(String studentFlowTrace) {
        this.studentFlowTrace = studentFlowTrace;
    }

    public String getCorrectFlowTrace() {
        return correctFlowTrace;
    }

    public void setCorrectFlowTrace(String correctFlowTrace) {
        this.correctFlowTrace = correctFlowTrace;
    }

    public String getKeyTakeaway() {
        return keyTakeaway;
    }

    public void setKeyTakeaway(String keyTakeaway) {
        this.keyTakeaway = keyTakeaway;
    }

    public String getSubmittedCode() {
        return submittedCode;
    }

    public void setSubmittedCode(String submittedCode) {
        this.submittedCode = submittedCode;
    }

    public String getUserReflections() {
        return userReflections;
    }

    public void setUserReflections(String userReflections) {
        this.userReflections = userReflections;
    }

    public MasteryStatus getMasteryStatus() {
        return masteryStatus;
    }

    public void setMasteryStatus(MasteryStatus masteryStatus) {
        this.masteryStatus = masteryStatus;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public Instant getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(Instant nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
