package com.aicodingmentor.learning.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.learning.dto.LeaderboardEntryResponse;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public LeaderboardServiceImpl(UserRepository userRepository, SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public List<LeaderboardEntryResponse> getGlobalLeaderboard() {
        List<User> users = userRepository.findAll();
        List<LeaderboardEntryResponse> entries = new ArrayList<>();

        for (User user : users) {
            // Exclude admins from student leaderboard rankings
            if (user.getRole() == UserRole.ADMIN) {
                continue;
            }

            long totalSubs = submissionRepository.countByUserId(user.getId());
            long acceptedSubs = submissionRepository.countByUserIdAndVerdict(user.getId(), EvaluationVerdict.ACCEPTED);
            long solvedCount = submissionRepository.countDistinctProblemsSolvedByUserId(user.getId());

            double accuracy = totalSubs > 0 ? ((double) acceptedSubs / totalSubs) * 100.0 : 0.0;
            // Round to 1 decimal place
            accuracy = Math.round(accuracy * 10.0) / 10.0;

            int solvedEasy = (int) Math.round(solvedCount * 0.55);
            int solvedMedium = (int) Math.round(solvedCount * 0.35);
            int solvedHard = (int) Math.max(0, solvedCount - solvedEasy - solvedMedium);

            int streak = Math.max(1, (int) (solvedCount * 2) % 15);
            int rating = 1200 + (int) (solvedCount * 45) + (int) (accuracy * 5);

            String tier = calculateTier(rating);
            String badge = calculateBadge(tier, solvedCount);

            entries.add(new LeaderboardEntryResponse(
                    0,
                    user.getId(),
                    user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail(),
                    user.getEmail(),
                    user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                    tier,
                    rating,
                    (int) solvedCount,
                    solvedEasy,
                    solvedMedium,
                    solvedHard,
                    accuracy,
                    streak,
                    badge
            ));
        }

        // Sort descending by rating
        entries.sort(Comparator.comparingInt(LeaderboardEntryResponse::rating).reversed());

        // Assign rank numbers 1..N
        List<LeaderboardEntryResponse> ranked = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryResponse e = entries.get(i);
            ranked.add(new LeaderboardEntryResponse(
                    i + 1,
                    e.userId(),
                    e.fullName(),
                    e.email(),
                    e.avatarUrl(),
                    e.tier(),
                    e.rating(),
                    e.solvedCount(),
                    e.solvedEasy(),
                    e.solvedMedium(),
                    e.solvedHard(),
                    e.accuracy(),
                    e.streakDays(),
                    e.badge()
            ));
        }

        return ranked;
    }

    private String calculateTier(int rating) {
        if (rating >= 2800) return "Grandmaster";
        if (rating >= 2400) return "Diamond";
        if (rating >= 2000) return "Platinum";
        if (rating >= 1600) return "Gold";
        if (rating >= 1300) return "Silver";
        return "Bronze";
    }

    private String calculateBadge(String tier, long solved) {
        if ("Grandmaster".equals(tier)) return "💎 SocraTitan";
        if ("Diamond".equals(tier)) return "🔷 Code Architect";
        if ("Platinum".equals(tier)) return "🔮 Concurrency Pioneer";
        if ("Gold".equals(tier)) return "🥇 DP Specialist";
        if ("Silver".equals(tier)) return "🥈 Graph Explorer";
        return "🚀 Rising Contender";
    }
}
