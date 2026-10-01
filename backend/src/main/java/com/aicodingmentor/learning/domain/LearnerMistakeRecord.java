package com.aicodingmentor.learning.domain;

import com.aicodingmentor.mistake.model.MistakeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity tracking recurring programming and algorithmic mistakes per learner.
 */
@Entity
@Table(name = "learner_mistakes")
public class LearnerMistakeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mistake_type", nullable = false, length = 100)
    private MistakeType mistakeType;

    @Column(name = "frequency", nullable = false)
    private int frequency = 1;

    @Column(name = "first_occurrence", nullable = false)
    private Instant firstOccurrence = Instant.now();

    @Column(name = "last_occurrence", nullable = false)
    private Instant lastOccurrence = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_mistake_topics", joinColumns = @JoinColumn(name = "mistake_id"))
    @Column(name = "topic", length = 100)
    private Set<String> relatedTopics = new HashSet<>();

    public LearnerMistakeRecord() {
    }

    public LearnerMistakeRecord(UUID userId, MistakeType mistakeType, Set<String> relatedTopics) {
        this.userId = userId;
        this.mistakeType = mistakeType;
        this.frequency = 1;
        this.firstOccurrence = Instant.now();
        this.lastOccurrence = Instant.now();
        if (relatedTopics != null) {
            this.relatedTopics = new HashSet<>(relatedTopics);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (firstOccurrence == null) {
            firstOccurrence = Instant.now();
        }
        if (lastOccurrence == null) {
            lastOccurrence = Instant.now();
        }
    }

    public void incrementFrequency(Set<String> newTopics) {
        this.frequency++;
        this.lastOccurrence = Instant.now();
        if (newTopics != null) {
            this.relatedTopics.addAll(newTopics);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public MistakeType getMistakeType() {
        return mistakeType;
    }

    public int getFrequency() {
        return frequency;
    }

    public Instant getFirstOccurrence() {
        return firstOccurrence;
    }

    public Instant getLastOccurrence() {
        return lastOccurrence;
    }

    public Set<String> getRelatedTopics() {
        return relatedTopics;
    }
}
