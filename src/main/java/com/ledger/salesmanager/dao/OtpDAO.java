package com.ledger.salesmanager.dao;

import com.ledger.salesmanager.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class OtpDAO {

    public void insertOtp(int userId, String code, LocalDateTime expiresAt) {
        String sql = "INSERT INTO otp_codes (user_id, otp_code, purpose, expires_at) VALUES (?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setString(3, "LOGIN_2FA");
            ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store OTP", e);
        }
    }

    /** Returns true and marks the OTP used if it is valid, unexpired, and unused. */
    public boolean verifyAndConsume(int userId, String code) {
        String select = "SELECT id, expires_at, is_used FROM otp_codes WHERE user_id=? AND otp_code=? " +
                "ORDER BY created_at DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(select)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                boolean used = rs.getBoolean("is_used");
                Timestamp expiresAt = rs.getTimestamp("expires_at");
                int otpId = rs.getInt("id");
                if (used || expiresAt.toLocalDateTime().isBefore(LocalDateTime.now())) return false;
                markUsed(con, otpId);
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify OTP", e);
        }
    }

    private void markUsed(Connection con, int otpId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE otp_codes SET is_used=TRUE WHERE id=?")) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    /** Counts OTP requests for a user within the last N minutes (rate limiting). */
    public int countRecentRequests(int userId, int withinMinutes) {
        String sql = "SELECT COUNT(*) FROM otp_codes WHERE user_id=? AND created_at >= (NOW() - INTERVAL ? MINUTE)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, withinMinutes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check OTP rate limit", e);
        }
        return 0;
    }
}
