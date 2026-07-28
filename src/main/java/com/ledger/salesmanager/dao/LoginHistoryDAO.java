package com.ledger.salesmanager.dao;

import com.ledger.salesmanager.config.DatabaseConnection;

import java.sql.*;

public class LoginHistoryDAO {

    public void record(int userId, String status, String ip) {
        String sql = "INSERT INTO login_history (user_id, status, ip_address) VALUES (?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, status);
            ps.setString(3, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record login history", e);
        }
    }
}
