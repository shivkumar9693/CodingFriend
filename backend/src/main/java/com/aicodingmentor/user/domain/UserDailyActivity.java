package com.aicodingmentor.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_daily_activity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_date"})
)
public class UserDailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "submission_count", nullable = false)
    private int submissionCount = 1;

    @Column(name = "problems_solved", nullable = false)
    private int problemsSolved = 0;

    public UserDailyActivity() {
    }

    public UserDailyActivity(UUID userId, LocalDate activityDate, int submissionCount, int problemsSolved) {
        this.userId = userId;
        this.activityDate = activityDate;
        this.submissionCount = submissionCount;
        this.problemsSolved = problemsSolved;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public int getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(int submissionCount) {
        this.submissionCount = submissionCount;
    }

    public int getProblemsSolved() {
        return problemsSolved;
    }

    public void setProblemsSolved(int problemsSolved) {
        this.problemsSolved = problemsSolved;
    }

    public void incrementSubmission() {
        this.submissionCount++;
    }

    public void incrementSolved() {
        this.problemsSolved++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDailyActivity that = (UserDailyActivity) o;
        return Objects.equals(userId, that.userId) && Objects.equals(activityDate, that.activityDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, activityDate);
    }
}
