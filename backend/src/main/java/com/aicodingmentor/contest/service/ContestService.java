package com.aicodingmentor.contest.service;

import com.aicodingmentor.contest.dto.ContestDto;
import com.aicodingmentor.contest.dto.ContestLeaderboardDto;
import com.aicodingmentor.contest.dto.ContestParticipantDto;
import com.aicodingmentor.contest.dto.CreateContestRequest;

import java.util.List;
import java.util.UUID;

public interface ContestService {

    List<ContestDto> getAllContests(UUID currentUserId);

    ContestDto getContestBySlug(String slug, UUID currentUserId);

    ContestDto createContest(CreateContestRequest request, UUID adminUserId);

    ContestParticipantDto registerForContest(String slug, UUID currentUserId);

    ContestLeaderboardDto getLeaderboard(String slug);

    void recordTabSwitch(String slug, UUID currentUserId);

    void recordSuccessfulProblemSubmission(UUID contestId, UUID problemId, UUID userId, int minutesFromStart);
}
