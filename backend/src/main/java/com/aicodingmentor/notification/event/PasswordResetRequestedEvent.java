package com.aicodingmentor.notification.event;

public record PasswordResetRequestedEvent(
        String toEmail,
        String userName,
        String resetLink
) {
}
