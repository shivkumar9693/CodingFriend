package com.aicodingmentor.notification.listener;

import com.aicodingmentor.notification.event.ContestCreatedNotificationEvent;
import com.aicodingmentor.notification.event.PasswordResetRequestedEvent;
import com.aicodingmentor.notification.service.EmailService;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneId.of("UTC"));

    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:https://codingfriend.vercel.app}")
    private String frontendUrl;

    public NotificationEventListener(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @Async
    @EventListener
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Handling PasswordResetRequestedEvent for email: {}", event.toEmail());
        emailService.sendPasswordResetEmail(event.toEmail(), event.userName(), event.resetLink());
    }

    @Async
    @EventListener
    public void handleContestCreated(ContestCreatedNotificationEvent event) {
        log.info("Broadcasting contest announcement for contest '{}' ({})", event.title(), event.contestId());

        String formattedTime = event.startTime() != null ? DATE_FORMATTER.format(event.startTime()) : "To be announced";
        String contestLink = frontendUrl + "/contests";

        try {
            List<User> activeUsers = userRepository.findAll();
            for (User user : activeUsers) {
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    emailService.sendContestAnnouncementEmail(
                            user.getEmail(),
                            user.getFullName(),
                            event.title(),
                            formattedTime,
                            event.durationMinutes(),
                            contestLink
                    );
                }
            }
            log.info("Finished scheduling contest emails for {} users", activeUsers.size());
        } catch (Exception e) {
            log.error("Failed to broadcast contest announcement: {}", e.getMessage(), e);
        }
    }
}
