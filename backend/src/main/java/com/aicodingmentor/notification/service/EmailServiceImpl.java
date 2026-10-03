package com.aicodingmentor.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder templateBuilder;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:CodingFriend <noreply@codingfriend.com>}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Autowired
    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender, EmailTemplateBuilder templateBuilder) {
        this.mailSender = mailSender;
        this.templateBuilder = templateBuilder;
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!mailEnabled || mailUsername == null || mailUsername.isBlank() || mailSender == null) {
            log.info("[DEV / SMTP DISABLED] Email to '{}' with subject '{}' suppressed. Body length: {} chars",
                    to, subject, htmlBody != null ? htmlBody.length() : 0);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress != null && !fromAddress.isBlank() ? fromAddress : mailUsername);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email successfully sent to '{}' with subject: '{}'", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to '{}' with subject '{}': {}", to, subject, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        String subject = "🔒 Reset Your CodingFriend Password";
        String htmlBody = templateBuilder.buildPasswordResetTemplate(userName, resetLink);
        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    @Override
    @Async
    public void sendContestAnnouncementEmail(String toEmail, String userName, String contestTitle, String startTime, Integer durationMinutes, String contestLink) {
        String subject = "🏆 New Contest: " + contestTitle + " on CodingFriend";
        String htmlBody = templateBuilder.buildContestAnnouncementTemplate(userName, contestTitle, startTime, durationMinutes, contestLink);
        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "🚀 Welcome to CodingFriend!";
        String htmlBody = templateBuilder.buildWelcomeTemplate(userName);
        sendHtmlEmail(toEmail, subject, htmlBody);
    }
}
