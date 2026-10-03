package com.aicodingmentor.notification.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailTemplateBuilder templateBuilder;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        templateBuilder = new EmailTemplateBuilder();
        emailService = new EmailServiceImpl(mailSender, templateBuilder, new com.fasterxml.jackson.databind.ObjectMapper());
        ReflectionTestUtils.setField(emailService, "mailUsername", "testapp@gmail.com");
        ReflectionTestUtils.setField(emailService, "fromAddress", "CodingFriend <testapp@gmail.com>");
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
    }

    @Test
    @DisplayName("sendPasswordResetEmail creates mime message and invokes mailSender.send")
    void testSendPasswordResetEmail() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("learner@example.com", "John Doe", "https://codingfriend.vercel.app/reset-password?token=abc");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendContestAnnouncementEmail creates mime message and invokes mailSender.send")
    void testSendContestAnnouncementEmail() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendContestAnnouncementEmail(
                "learner@example.com",
                "John Doe",
                "Weekly Contest 42",
                "2026-10-04 14:00 UTC",
                90,
                "https://codingfriend.vercel.app/contests"
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendHtmlEmail gracefully handles disabled SMTP without throwing")
    void testSendHtmlEmailWhenDisabled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        emailService.sendHtmlEmail("learner@example.com", "Test Subject", "<p>Hello</p>");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
