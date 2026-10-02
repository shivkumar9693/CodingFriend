package com.aicodingmentor.learning.domain;

/**
 * Status of a concept/counter-example note in the learner's spaced repetition revision loop.
 */
public enum MasteryStatus {
    TO_REVISE,
    REVISED,
    MASTERED,
    ARCHIVED
}
