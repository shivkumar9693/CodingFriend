package com.aicodingmentor.learning.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.learning.domain.LearnerMistakeRecord;
import com.aicodingmentor.learning.dto.LearnerMistakeSummaryResponse;
import com.aicodingmentor.learning.dto.LearnerStatisticsResponse;
import com.aicodingmentor.learning.repository.LearnerMistakeRepository;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class LearnerMistakeTrackingServiceImpl implements LearnerMistakeTrackingService {

    private static final Logger log = LoggerFactory.getLogger(LearnerMistakeTrackingServiceImpl.class);

    private final LearnerMistakeRepository mistakeRepository;
    private final SubmissionRepository submissionRepository;

    public LearnerMistakeTrackingServiceImpl(
            LearnerMistakeRepository mistakeRepository,
            SubmissionRepository submissionRepository
    ) {
        this.mistakeRepository = mistakeRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    @Transactional
    public void recordMistake(UUID userId, MistakeType mistakeType, Set<String> relatedTopics) {
        if (userId == null || mistakeType == null || mistakeType == MistakeType.NONE) {
            return;
        }

        Optional<LearnerMistakeRecord> existing = mistakeRepository.findFirstByUserIdAndMistakeType(userId, mistakeType);

        if (existing.isPresent()) {
            LearnerMistakeRecord record = existing.get();
            record.incrementFrequency(relatedTopics);
            mistakeRepository.save(record);
            log.info("Incremented mistake [{}] for user [{}], new frequency [{}]",
                    mistakeType, userId, record.getFrequency());
        } else {
            LearnerMistakeRecord newRecord = new LearnerMistakeRecord(userId, mistakeType, relatedTopics);
            mistakeRepository.save(newRecord);
            log.info("Recorded new mistake pattern [{}] for user [{}]", mistakeType, userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearnerMistakeSummaryResponse> getLearnerMistakes(UUID userId) {
        return mistakeRepository.findByUserIdOrderByFrequencyDesc(userId).stream()
                .map(LearnerMistakeSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerStatisticsResponse getLearnerStatistics(UUID userId) {
        long totalSubmissions = submissionRepository.countByUserId(userId);
        long totalAccepted = submissionRepository.countByUserIdAndVerdict(userId, EvaluationVerdict.ACCEPTED);
        long problemsSolved = submissionRepository.countDistinctProblemsSolvedByUserId(userId);
        long problemsAttempted = submissionRepository.countDistinctProblemsAttemptedByUserId(userId);

        double accuracy = totalSubmissions > 0
                ? Math.round(((double) totalAccepted / totalSubmissions * 100.0) * 100.0) / 100.0
                : 0.0;

        double averageAttempts = problemsAttempted > 0
                ? Math.round(((double) totalSubmissions / problemsAttempted) * 10.0) / 10.0
                : 0.0;

        List<LearnerMistakeRecord> records = mistakeRepository.findByUserIdOrderByFrequencyDesc(userId);
        Map<MistakeType, Integer> breakdown = new EnumMap<>(MistakeType.class);
        Map<String, Integer> weakTopics = new java.util.LinkedHashMap<>();

        MistakeType topMistake = null;
        int maxFreq = 0;

        for (LearnerMistakeRecord record : records) {
            breakdown.put(record.getMistakeType(), record.getFrequency());
            if (record.getFrequency() > maxFreq) {
                maxFreq = record.getFrequency();
                topMistake = record.getMistakeType();
            }

            if (record.getRelatedTopics() != null) {
                for (String topic : record.getRelatedTopics()) {
                    weakTopics.merge(topic, record.getFrequency(), Integer::sum);
                }
            }
        }

        return new LearnerStatisticsResponse(
                totalSubmissions,
                totalAccepted,
                accuracy,
                problemsSolved,
                problemsAttempted,
                averageAttempts,
                topMistake,
                breakdown,
                weakTopics
        );
    }
}
