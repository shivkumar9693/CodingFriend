package com.aicodingmentor.notification;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SmtpLiveVerificationTest {

    @Test
    @org.junit.jupiter.api.Disabled("Verified live - disabled for automated CI")
    @DisplayName("Verify live Google SMTP connection and deliver test email")
    void testLiveGoogleSmtpConnection() {
        assertDoesNotThrow(() -> {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost("smtp.gmail.com");
            mailSender.setPort(587);
            mailSender.setUsername("shivkumarthakur9693@gmail.com");
            mailSender.setPassword("lsygifqahmperwtq");

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("CodingFriend <shivkumarthakur9693@gmail.com>");
            helper.setTo("shivkumarthakur9693@gmail.com");
            helper.setSubject("🚀 CodingFriend Google SMTP Live Verification");
            helper.setText("<h2>Google SMTP is working perfectly!</h2><p>Your CodingFriend email notification system is configured and verified.</p>", true);

            mailSender.send(message);
            System.out.println(">>> SUCCESS: Test email dispatched successfully via smtp.gmail.com:587 <<<");
        });
    }
}
