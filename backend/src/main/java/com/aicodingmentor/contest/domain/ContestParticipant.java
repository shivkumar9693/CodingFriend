package com.aicodingmentor.contest.domain;

import com.aicodingmentor.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "contest_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contest_id", "user_id"})
)
public class ContestParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_score", nullable = false)
    private int totalScore = 0;

    @Column(name = "total_penalty_minutes", nullable = false)
    private int totalPenaltyMinutes = 0;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Column(name = "rating_before")
    private Integer ratingBefore = 1200;

    @Column(name = "rating_after")
    private Integer ratingAfter = 1200;

    @Column(name = "tab_switch_count", nullable = false)
    private int tabSwitchCount = 0;

    public ContestParticipant() {
    }

    public ContestParticipant(Contest contest, User user) {
        this.contest = contest;
        this.user = user;
        this.registeredAt = Instant.now();
        this.ratingBefore = user.getContestRating();
        this.ratingAfter = user.getContestRating();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Contest getContest() {
        return contest;
    }

    public void setContest(Contest contest) {
        this.contest = contest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalPenaltyMinutes() {
        return totalPenaltyMinutes;
    }

    public void setTotalPenaltyMinutes(int totalPenaltyMinutes) {
        this.totalPenaltyMinutes = totalPenaltyMinutes;
    }

    public Integer getFinalRank() {
        return finalRank;
    }

    public void setFinalRank(Integer finalRank) {
        this.finalRank = finalRank;
    }

    public Integer getRatingBefore() {
        return ratingBefore;
    }

    public void setRatingBefore(Integer ratingBefore) {
        this.ratingBefore = ratingBefore;
    }

    public Integer getRatingAfter() {
        return ratingAfter;
    }

    public void setRatingAfter(Integer ratingAfter) {
        this.ratingAfter = ratingAfter;
    }

    public int getTabSwitchCount() {
        return tabSwitchCount;
    }

    public void setTabSwitchCount(int tabSwitchCount) {
        this.tabSwitchCount = tabSwitchCount;
    }

    public void incrementTabSwitch() {
        this.tabSwitchCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContestParticipant that = (ContestParticipant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
