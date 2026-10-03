package com.aicodingmentor.notification.event;

import java.time.Instant;
import java.util.UUID;

public record ContestCreatedNotificationEvent(
        UUID contestId,
        String title,
        Instant startTime,
        Integer durationMinutes
) {
}
