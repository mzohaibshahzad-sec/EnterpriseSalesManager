package com.ledger.salesmanager.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection provider. Kept intentionally simple (one
 * connection per call) rather than a full pool — swap in HikariCP here
 * if you need higher concurrency for a multi-terminal deployment.
 */
public class DatabaseConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found on classpath", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        AppConfig cfg = AppConfig.getInstance();
        return DriverManager.getConnection(cfg.getJdbcUrl(), cfg.getDbUser(), cfg.getDbPassword());
    }

    /** Used by the Setup Wizard to verify credentials before saving them. */
    public static boolean testConnection(String host, String port, String name, String user, String password) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            return c.isValid(3);
        } catch (SQLException e) {
            System.err.println("DB connection test failed: " + e.getMessage());
            return false;
        }
    }
}
