package com.aicodingmentor.learning.repository;

import com.aicodingmentor.learning.domain.LearnerMistakeRecord;
import com.aicodingmentor.mistake.model.MistakeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerMistakeRepository extends JpaRepository<LearnerMistakeRecord, UUID> {

    List<LearnerMistakeRecord> findByUserIdOrderByFrequencyDesc(UUID userId);

    Optional<LearnerMistakeRecord> findFirstByUserIdAndMistakeType(UUID userId, MistakeType mistakeType);

    Optional<LearnerMistakeRecord> findByUserIdAndMistakeType(UUID userId, MistakeType mistakeType);
}
