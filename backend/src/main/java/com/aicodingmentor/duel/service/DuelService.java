package com.aicodingmentor.duel.service;

import com.aicodingmentor.duel.dto.CreateDuelRequest;
import com.aicodingmentor.duel.dto.DuelDto;

import java.util.List;
import java.util.UUID;

public interface DuelService {

    DuelDto createOrQueueDuel(CreateDuelRequest request, UUID userId);

    DuelDto joinDuelRoom(String roomCode, UUID userId);

    DuelDto getDuelByRoomCode(String roomCode);

    DuelDto updateDuelProgress(String roomCode, UUID userId, int progressPercent);

    DuelDto completeDuel(String roomCode, UUID winnerId);

    void leaveOrCancelDuel(String roomCode, UUID userId);

    int cleanupOldDuels(int olderThanHours);

    List<DuelDto> getUserDuelHistory(UUID userId);
}
