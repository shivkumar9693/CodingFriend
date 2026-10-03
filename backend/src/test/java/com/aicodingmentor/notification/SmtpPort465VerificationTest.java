package com.aicodingmentor.notification;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SmtpPort465VerificationTest {

    @Test
    @org.junit.jupiter.api.Disabled("Verified live - disabled for automated CI")
    @DisplayName("Verify live Google SMTP connection over Port 465 SSL")
    void testGoogleSmtpPort465Ssl() {
        assertDoesNotThrow(() -> {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost("smtp.gmail.com");
            mailSender.setPort(465);
            mailSender.setUsername("shivkumarthakur9693@gmail.com");
            mailSender.setPassword("lsygifqahmperwtq");

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("CodingFriend <shivkumarthakur9693@gmail.com>");
            helper.setTo("shivkumarthakur9693@gmail.com");
            helper.setSubject("🚀 CodingFriend Google SMTP Port 465 SSL Verification");
            helper.setText("<h2>Google SMTP over Port 465 SSL is Working!</h2><p>This port bypasses cloud network firewalls like Render and AWS.</p>", true);

            mailSender.send(message);
            System.out.println(">>> SUCCESS: Test email dispatched successfully via smtp.gmail.com:465 (SSL) <<<");
        });
    }
}
