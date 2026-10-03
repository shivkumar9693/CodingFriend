package com.aicodingmentor.notification.service;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendPasswordResetEmail(String toEmail, String userName, String resetLink);
    void sendContestAnnouncementEmail(String toEmail, String userName, String contestTitle, String startTime, Integer durationMinutes, String contestLink);
    void sendWelcomeEmail(String toEmail, String userName);
}
