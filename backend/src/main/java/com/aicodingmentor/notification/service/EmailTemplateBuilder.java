package com.aicodingmentor.notification.service;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildPasswordResetTemplate(String userName, String resetLink) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : "Learner";
        String template = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Reset Your CodingFriend Password</title>
              <style>
                body { margin: 0; padding: 0; background-color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc; }
                .container { max-width: 580px; margin: 30px auto; background: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%); padding: 32px 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; }
                .content { padding: 32px 28px; line-height: 1.6; color: #cbd5e1; }
                .greeting { font-size: 18px; font-weight: 600; color: #f8fafc; margin-bottom: 16px; }
                .btn-container { text-align: center; margin: 32px 0; }
                .btn { display: inline-block; background: #3b82f6; color: #ffffff !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600; font-size: 15px; }
                .alert-box { background: #0f172a; border-left: 4px solid #f59e0b; padding: 14px 18px; border-radius: 6px; margin: 24px 0; font-size: 13px; color: #94a3b8; }
                .link-backup { word-break: break-all; font-size: 12px; color: #64748b; margin-top: 24px; }
                .footer { background: #0f172a; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #334155; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>CodingFriend</h1>
                  <p style="margin: 6px 0 0 0; color: #e0e7ff; font-size: 14px;">AI-Powered Java Learning Platform</p>
                </div>
                <div class="content">
                  <div class="greeting">Hello, {{USER_NAME}}! 👋</div>
                  <p>We received a request to reset the password for your <strong>CodingFriend</strong> account. Click the button below to set a new password:</p>
                  
                  <div class="btn-container">
                    <a href="{{RESET_LINK}}" class="btn" target="_blank">Reset Password</a>
                  </div>
                  
                  <div class="alert-box">
                    ⏱️ <strong>Note:</strong> This link is valid for <strong>30 minutes</strong> and can only be used once.<br>
                    🔒 If you did not request this password reset, you can safely ignore this email — your account remains secure.
                  </div>
                  
                  <div class="link-backup">
                    If the button above doesn't work, copy and paste this URL into your browser:<br>
                    <a href="{{RESET_LINK}}" style="color: #60a5fa;">{{RESET_LINK}}</a>
                  </div>
                </div>
                <div class="footer">
                  © 2026 CodingFriend. All rights reserved.<br>
                  Empowering engineers through AI Socratic mentorship.
                </div>
              </div>
            </body>
            </html>
            """;
        return template
                .replace("{{USER_NAME}}", displayName)
                .replace("{{RESET_LINK}}", resetLink != null ? resetLink : "");
    }

    public String buildContestAnnouncementTemplate(String userName, String contestTitle, String startTime, Integer durationMinutes, String contestLink) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : "Learner";
        String durationText = durationMinutes != null ? durationMinutes + " Minutes" : "Standard Duration";
        String template = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>New Coding Contest Announced: {{CONTEST_TITLE}}</title>
              <style>
                body { margin: 0; padding: 0; background-color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc; }
                .container { max-width: 580px; margin: 30px auto; background: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%); padding: 32px 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; }
                .content { padding: 32px 28px; line-height: 1.6; color: #cbd5e1; }
                .contest-card { background: #0f172a; border-radius: 8px; border: 1px solid #334155; padding: 20px; margin: 24px 0; }
                .contest-title { font-size: 18px; font-weight: 700; color: #38bdf8; margin-bottom: 12px; }
                .detail-row { margin: 8px 0; font-size: 14px; }
                .detail-label { color: #94a3b8; font-weight: 500; }
                .detail-value { color: #f8fafc; font-weight: 600; margin-left: 8px; }
                .btn-container { text-align: center; margin: 32px 0; }
                .btn { display: inline-block; background: #8b5cf6; color: #ffffff !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600; font-size: 15px; }
                .footer { background: #0f172a; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #334155; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🏆 New Contest Announced!</h1>
                  <p style="margin: 6px 0 0 0; color: #fdf2f8; font-size: 14px;">Compete, climb the leaderboard, and boost your rating</p>
                </div>
                <div class="content">
                  <div style="font-size: 18px; font-weight: 600; color: #f8fafc; margin-bottom: 16px;">Hey {{USER_NAME}}! 🚀</div>
                  <p>A new competitive coding contest has just been scheduled on <strong>CodingFriend</strong>. Test your algorithmic problem-solving skills against peers in real-time!</p>
                  
                  <div class="contest-card">
                    <div class="contest-title">{{CONTEST_TITLE}}</div>
                    <div class="detail-row"><span class="detail-label">🕒 Starts At:</span><span class="detail-value">{{START_TIME}}</span></div>
                    <div class="detail-row"><span class="detail-label">⏳ Duration:</span><span class="detail-value">{{DURATION}}</span></div>
                    <div class="detail-row"><span class="detail-label">📈 Rating:</span><span class="detail-value">Rated for all participants</span></div>
                  </div>
                  
                  <div class="btn-container">
                    <a href="{{CONTEST_LINK}}" class="btn" target="_blank">Register & View Contest</a>
                  </div>
                  
                  <p style="font-size: 13px; color: #94a3b8; text-align: center;">Make sure you are logged in to register before the countdown ends.</p>
                </div>
                <div class="footer">
                  © 2026 CodingFriend. Happy Coding!<br>
                  To manage your email preferences, visit your account settings.
                </div>
              </div>
            </body>
            </html>
            """;
        return template
                .replace("{{USER_NAME}}", displayName)
                .replace("{{CONTEST_TITLE}}", contestTitle != null ? contestTitle : "Contest")
                .replace("{{START_TIME}}", startTime != null ? startTime : "TBA")
                .replace("{{DURATION}}", durationText)
                .replace("{{CONTEST_LINK}}", contestLink != null ? contestLink : "");
    }

    public String buildWelcomeTemplate(String userName) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : "Learner";
        String template = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Welcome to CodingFriend</title>
              <style>
                body { margin: 0; padding: 0; background-color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc; }
                .container { max-width: 580px; margin: 30px auto; background: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%); padding: 32px 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; }
                .content { padding: 32px 28px; line-height: 1.6; color: #cbd5e1; }
                .feature-list { margin: 20px 0; padding-left: 20px; }
                .feature-list li { margin: 10px 0; }
                .footer { background: #0f172a; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #334155; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Welcome to CodingFriend! 🚀</h1>
                  <p style="margin: 6px 0 0 0; color: #ecfdf5; font-size: 14px;">Your AI-powered coding journey begins now</p>
                </div>
                <div class="content">
                  <div style="font-size: 18px; font-weight: 600; color: #f8fafc; margin-bottom: 16px;">Welcome aboard, {{USER_NAME}}! 👋</div>
                  <p>You’ve joined a community focused on deep understanding and algorithmic mastery. Here's how you can make the most out of CodingFriend:</p>
                  
                  <ul class="feature-list">
                    <li>💡 <strong>Socratic AI Mentor:</strong> Receive step-by-step hints and conceptual insights without spoiling full solutions.</li>
                    <li>🔍 <strong>Visual Execution Debugger:</strong> Step through variables and control flow in real time.</li>
                    <li>⚔️ <strong>1v1 Coding Duels & Contests:</strong> Put your speed and accuracy to the test against peers.</li>
                  </ul>
                  
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="https://codingfriend.vercel.app/problems" style="display: inline-block; background: #10b981; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600;">Explore Problems</a>
                  </div>
                </div>
                <div class="footer">
                  © 2026 CodingFriend. Happy Coding!
                </div>
              </div>
            </body>
            </html>
            """;
        return template.replace("{{USER_NAME}}", displayName);
    }
}
