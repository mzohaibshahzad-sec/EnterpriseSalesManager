package com.ledger.salesmanager.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Loads and persists application configuration to a local file:
 *   ~/.enterprise-sales-manager/config.properties
 *
 * This file is created by the Setup Wizard on first launch and is what
 * makes the app "remember" that setup is complete. It also stores the
 * MySQL connection details and the SMTP credentials used to send the
 * Owner's 2FA OTP emails.
 *
 * IMPORTANT (security note for the developer running this locally):
 *   - Never commit config.properties to version control.
 *   - The SMTP password should be a Gmail "App Password", not your
 *     real Gmail account password (Google requires this for SMTP apps).
 */
public class AppConfig {

    private static final Path CONFIG_DIR =
            Paths.get(System.getProperty("user.home"), ".enterprise-sales-manager");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private static AppConfig instance;
    private final Properties props = new Properties();

    private AppConfig() {
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    private void load() {
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read config.properties: " + e.getMessage());
            }
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Enterprise Sales Manager configuration");
            }
        } catch (IOException e) {
            System.err.println("Could not write config.properties: " + e.getMessage());
        }
    }

    public boolean isSetupCompleted() {
        return Boolean.parseBoolean(props.getProperty("setup.completed", "false"));
    }

    public void markSetupCompleted() {
        props.setProperty("setup.completed", "true");
        save();
    }

    // ---- Database ----
    public String getDbHost()     { return props.getProperty("db.host", "localhost"); }
    public String getDbPort()     { return props.getProperty("db.port", "3306"); }
    public String getDbName()     { return props.getProperty("db.name", "sales_management"); }
    public String getDbUser()     { return props.getProperty("db.user", "root"); }
    public String getDbPassword() { return props.getProperty("db.password", ""); }

    public void setDatabaseCredentials(String host, String port, String name, String user, String password) {
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.name", name);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password);
        save();
    }

    public String getJdbcUrl() {
        return "jdbc:mysql://" + getDbHost() + ":" + getDbPort() + "/" + getDbName()
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    // ---- SMTP (Gmail) for OTP emails ----
    public String getSmtpHost()     { return props.getProperty("smtp.host", "smtp.gmail.com"); }
    public String getSmtpPort()     { return props.getProperty("smtp.port", "587"); }
    public String getSmtpUsername() { return props.getProperty("smtp.username", ""); }
    public String getSmtpPassword() { return props.getProperty("smtp.password", ""); }

    public void setSmtpCredentials(String username, String appPassword) {
        props.setProperty("smtp.username", username);
        props.setProperty("smtp.password", appPassword);
        save();
    }

    public boolean isSmtpConfigured() {
        return !getSmtpUsername().isBlank() && !getSmtpPassword().isBlank();
    }

    // ---- UI ----
    public String getTheme() { return props.getProperty("ui.theme", "DARK"); }
    public void setTheme(String theme) {
        props.setProperty("ui.theme", theme);
        save();
    }
}
