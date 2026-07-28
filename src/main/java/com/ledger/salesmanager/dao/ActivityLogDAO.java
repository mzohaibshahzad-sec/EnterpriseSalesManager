package com.ledger.salesmanager.dao;

import com.ledger.salesmanager.config.DatabaseConnection;
import com.ledger.salesmanager.model.ActivityLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    public void log(Integer userId, String action, String details) {
        String sql = "INSERT INTO activity_logs (user_id, action, details) VALUES (?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to write activity log", e);
        }
    }

    public List<ActivityLog> recent(int limit) {
        return search(null, null, null, null, limit);
    }

    /**
     * Flexible query for the Audit Log screen: any parameter left null is
     * skipped. userFilter matches full name (LIKE), actionFilter is an
     * exact match against the action code (e.g. "SALE_RECORDED").
     */
    public List<ActivityLog> search(String userFilter, String actionFilter, LocalDate from, LocalDate to, int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT al.*, u.full_name FROM activity_logs al " +
                "LEFT JOIN users u ON al.user_id = u.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (userFilter != null && !userFilter.isBlank()) {
            sql.append("AND u.full_name LIKE ? ");
            params.add("%" + userFilter + "%");
        }
        if (actionFilter != null && !actionFilter.isBlank() && !actionFilter.equalsIgnoreCase("ALL")) {
            sql.append("AND al.action = ? ");
            params.add(actionFilter);
        }
        if (from != null) {
            sql.append("AND DATE(al.created_at) >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append("AND DATE(al.created_at) <= ? ");
            params.add(Date.valueOf(to));
        }
        sql.append("ORDER BY al.created_at DESC LIMIT ?");
        params.add(limit);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) logs.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search activity logs", e);
        }
        return logs;
    }

    /** Distinct action codes seen so far, for populating the Audit Log filter dropdown. */
    public List<String> distinctActions() {
        List<String> actions = new ArrayList<>();
        String sql = "SELECT DISTINCT action FROM activity_logs ORDER BY action";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) actions.add(rs.getString(1));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load audit action types", e);
        }
        return actions;
    }

    private ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        int uid = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : uid);
        log.setUserName(rs.getString("full_name"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) log.setCreatedAt(ts.toLocalDateTime());
        return log;
    }
}
