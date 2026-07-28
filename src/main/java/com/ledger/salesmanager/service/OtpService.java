package com.ledger.salesmanager.service;

import com.ledger.salesmanager.config.AppConfig;
import com.ledger.salesmanager.dao.OtpDAO;
import com.ledger.salesmanager.model.User;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Implements mandatory Email OTP 2FA for the Owner role, per spec:
 *   - 6-digit numeric OTP
 *   - expires after 5 minutes
 *   - single use only
 *   - rate-limited resend
 *   - every attempt is logged (via OtpDAO / ActivityLogDAO)
 *
 * Requires SMTP credentials to be configured in AppConfig (Settings screen):
 * a Gmail address + a Gmail "App Password" (not the account password —
 * Google blocks plain-password SMTP logins for security).
 */
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;

    private final OtpDAO otpDAO = new OtpDAO();
    private final SecureRandom random = new SecureRandom();

    public enum SendResult { SENT, RATE_LIMITED, SMTP_NOT_CONFIGURED, SEND_FAILED }

    public SendResult sendLoginOtp(User user) {
        if (!AppConfig.getInstance().isSmtpConfigured()) {
            return SendResult.SMTP_NOT_CONFIGURED;
        }
        if (otpDAO.countRecentRequests(user.getId(), RATE_LIMIT_WINDOW_MINUTES) >= MAX_REQUESTS_PER_WINDOW) {
            return SendResult.RATE_LIMITED;
        }

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);
        otpDAO.insertOtp(user.getId(), code, expiresAt);

        try {
            sendEmail(user.getGmail(), code);
            return SendResult.SENT;
        } catch (MessagingException e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            return SendResult.SEND_FAILED;
        }
    }

    public boolean verifyOtp(User user, String enteredCode) {
        return otpDAO.verifyAndConsume(user.getId(), enteredCode.trim());
    }

    private String generateCode() {
        int number = random.nextInt(1_000_000);
        return String.format("%0" + OTP_LENGTH + "d", number);
    }

    private void sendEmail(String toGmail, String code) throws MessagingException {
        AppConfig cfg = AppConfig.getInstance();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", cfg.getSmtpHost());
        props.put("mail.smtp.port", cfg.getSmtpPort());

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.getSmtpUsername(), cfg.getSmtpPassword());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(cfg.getSmtpUsername()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toGmail));
        message.setSubject("Your Login Verification Code");
        message.setText(
                "Your one-time verification code is: " + code + "\n\n" +
                "This code expires in " + EXPIRY_MINUTES + " minutes and can only be used once.\n" +
                "If you did not request this, please secure your account immediately.\n\n" +
                "— Enterprise Sales Manager Security"
        );

        Transport.send(message);
    }
}
