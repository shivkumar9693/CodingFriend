package com.aicodingmentor.duel.repository;

import com.aicodingmentor.duel.domain.Duel;
import com.aicodingmentor.duel.domain.DuelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DuelRepository extends JpaRepository<Duel, UUID> {

    Optional<Duel> findByRoomCode(String roomCode);

    List<Duel> findByStatus(DuelStatus status);

    @Query("SELECT d FROM Duel d WHERE d.status = 'WAITING' AND d.difficulty = :difficulty AND d.player1.id <> :userId ORDER BY d.createdAt ASC")
    List<Duel> findOpenQueueDuels(@Param("difficulty") String difficulty, @Param("userId") UUID userId);

    @Query("SELECT d FROM Duel d WHERE d.player1.id = :userId OR d.player2.id = :userId ORDER BY d.createdAt DESC")
    List<Duel> findUserDuelHistory(@Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Duel d WHERE (d.status = 'COMPLETED' AND d.createdAt < :cutoff) OR (d.status = 'WAITING' AND d.createdAt < :cutoff)")
    int deleteOldCompletedDuels(@Param("cutoff") java.time.Instant cutoff);
}
