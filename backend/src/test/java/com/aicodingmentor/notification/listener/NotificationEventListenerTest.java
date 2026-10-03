package com.aicodingmentor.notification.listener;

import com.aicodingmentor.notification.event.ContestCreatedNotificationEvent;
import com.aicodingmentor.notification.event.PasswordResetRequestedEvent;
import com.aicodingmentor.notification.service.EmailService;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(emailService, userRepository);
        ReflectionTestUtils.setField(listener, "frontendUrl", "https://codingfriend.vercel.app");
    }

    @Test
    @DisplayName("handlePasswordResetRequested invokes emailService.sendPasswordResetEmail")
    void testHandlePasswordResetRequested() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "learner@example.com",
                "Learner",
                "https://codingfriend.vercel.app/reset-password?token=xyz"
        );

        listener.handlePasswordResetRequested(event);

        verify(emailService).sendPasswordResetEmail(
                eq("learner@example.com"),
                eq("Learner"),
                eq("https://codingfriend.vercel.app/reset-password?token=xyz")
        );
    }

    @Test
    @DisplayName("handleContestCreated sends email to all registered users")
    void testHandleContestCreated() {
        User user1 = new User("user1@example.com", "pass", "User One", UserRole.USER);
        User user2 = new User("user2@example.com", "pass", "User Two", UserRole.USER);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        ContestCreatedNotificationEvent event = new ContestCreatedNotificationEvent(
                UUID.randomUUID(),
                "Spring Code Clash",
                Instant.now(),
                90
        );

        listener.handleContestCreated(event);

        verify(emailService, times(2)).sendContestAnnouncementEmail(
                anyString(),
                anyString(),
                eq("Spring Code Clash"),
                anyString(),
                eq(90),
                eq("https://codingfriend.vercel.app/contests")
        );
    }
}
