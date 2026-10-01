package com.aicodingmentor.mentor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity persisting Socratic hints generated for learner submissions.
 */
@Entity
@Table(name = "submission_hints")
public class SubmissionHint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "hint_level", nullable = false)
    private int hintLevel;

    @Column(name = "mistake_type", length = 100)
    private String mistakeType;

    @Column(name = "hint", nullable = false, columnDefinition = "TEXT")
    private String hint;

    @Column(name = "concept", columnDefinition = "TEXT")
    private String concept;

    @Column(name = "reveals_solution", nullable = false)
    private boolean revealsSolution = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SubmissionHint() {
    }

    public SubmissionHint(UUID submissionId, UUID userId, UUID problemId, int hintLevel, String mistakeType, String hint, String concept, boolean revealsSolution) {
        this.submissionId = submissionId;
        this.userId = userId;
        this.problemId = problemId;
        this.hintLevel = hintLevel;
        this.mistakeType = mistakeType;
        this.hint = hint;
        this.concept = concept;
        this.revealsSolution = revealsSolution;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public int getHintLevel() {
        return hintLevel;
    }

    public String getMistakeType() {
        return mistakeType;
    }

    public String getHint() {
        return hint;
    }

    public String getConcept() {
        return concept;
    }

    public boolean isRevealsSolution() {
        return revealsSolution;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
