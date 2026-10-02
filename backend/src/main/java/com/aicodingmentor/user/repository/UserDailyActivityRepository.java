package com.aicodingmentor.user.repository;

import com.aicodingmentor.user.domain.UserDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDailyActivityRepository extends JpaRepository<UserDailyActivity, UUID> {

    Optional<UserDailyActivity> findByUserIdAndActivityDate(UUID userId, LocalDate activityDate);

    @Query("SELECT a FROM UserDailyActivity a WHERE a.userId = :userId AND a.activityDate >= :startDate ORDER BY a.activityDate ASC")
    List<UserDailyActivity> findActivitiesSince(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate);

    @Query("SELECT a FROM UserDailyActivity a WHERE a.userId = :userId ORDER BY a.activityDate DESC")
    List<UserDailyActivity> findAllByUserIdDesc(@Param("userId") UUID userId);
}
