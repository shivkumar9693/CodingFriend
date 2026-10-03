package com.aicodingmentor.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder templateBuilder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:CodingFriend <shivkumarthakur9693@gmail.com>}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Autowired
    public EmailServiceImpl(
            @Autowired(required = false) JavaMailSender mailSender,
            EmailTemplateBuilder templateBuilder,
            ObjectMapper objectMapper
    ) {
        this.mailSender = mailSender;
        this.templateBuilder = templateBuilder;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("[EMAIL DISABLED] Email to '{}' with subject '{}' suppressed.", to, subject);
            return;
        }

        // 1. Primary Strategy for Cloud/Production: Brevo HTTPS REST API (Port 443 - Never blocked by Render)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(to, subject, htmlBody);
            if (sent) {
                return;
            }
            log.warn("Brevo HTTPS API delivery failed. Attempting fallback to SMTP...");
        }

        // 2. Secondary Strategy for Local Dev: Standard JavaMailSender (SMTP)
        if (mailUsername != null && !mailUsername.isBlank() && mailSender != null) {
            sendViaSmtp(to, subject, htmlBody);
            return;
        }

        log.info("[DEV / NO EMAIL SERVICE CONFIGURED] Email to '{}' with subject '{}' logged. Body length: {} chars",
                to, subject, htmlBody != null ? htmlBody.length() : 0);
    }

    private boolean sendViaBrevoApi(String to, String subject, String htmlBody) {
        String senderName = "CodingFriend";
        String senderEmail = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : "shivkumarthakur9693@gmail.com";

        if (fromAddress != null && fromAddress.contains("<") && fromAddress.contains(">")) {
            senderName = fromAddress.substring(0, fromAddress.indexOf("<")).trim();
            senderEmail = fromAddress.substring(fromAddress.indexOf("<") + 1, fromAddress.indexOf(">")).trim();
        } else if (fromAddress != null && !fromAddress.isBlank()) {
            senderEmail = fromAddress.trim();
        }

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email successfully sent via Brevo HTTPS API to '{}' (HTTP {}). Response: {}",
                        to, response.statusCode(), response.body());
                return true;
            } else {
                log.error("Brevo API returned HTTP {} for email to '{}': {}", response.statusCode(), to, response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send email via Brevo HTTPS API to '{}': {}", to, e.getMessage(), e);
            return false;
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress != null && !fromAddress.isBlank() ? fromAddress : mailUsername);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email successfully sent via SMTP to '{}' with subject: '{}'", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email via SMTP to '{}' with subject '{}': {}", to, subject, e.getMessage());
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
