package com.ledger.salesmanager.service;

import com.ledger.salesmanager.config.AppConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the `mysqldump` / `mysql` command-line clients to provide backup
 * and restore. These must be installed and on the system PATH — they
 * ship with any standard MySQL Server / MySQL Workbench installation.
 *
 * Note: shelling out is intentional here — it reuses MySQL's own,
 * battle-tested dump format instead of reinventing SQL serialization.
 */
public class BackupService {

    public static class BackupException extends RuntimeException {
        public BackupException(String message, Throwable cause) { super(message, cause); }
        public BackupException(String message) { super(message); }
    }

    /** Creates a full .sql dump of the configured database at the given path. */
    public Path backup(Path targetDirectory) {
        AppConfig cfg = AppConfig.getInstance();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path outputFile = targetDirectory.resolve("backup_" + cfg.getDbName() + "_" + timestamp + ".sql");

        List<String> command = new ArrayList<>(List.of(
                "mysqldump",
                "-h", cfg.getDbHost(),
                "-P", cfg.getDbPort(),
                "-u", cfg.getDbUser(),
                "--result-file=" + outputFile,
                cfg.getDbName()
        ));
        // Password is passed via env var (MYSQL_PWD) rather than argv to avoid
        // it showing up in process listings like `ps aux`.
        runProcess(command, cfg.getDbPassword());
        return outputFile;
    }

    /** Restores the database from a previously created .sql dump. DESTRUCTIVE — confirm with the user first. */
    public void restore(Path sqlFile) {
        AppConfig cfg = AppConfig.getInstance();
        List<String> command = List.of(
                "mysql",
                "-h", cfg.getDbHost(),
                "-P", cfg.getDbPort(),
                "-u", cfg.getDbUser(),
                cfg.getDbName()
        );
        runProcessWithFileInput(command, cfg.getDbPassword(), sqlFile);
    }

    private void runProcess(List<String> command, String dbPassword) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("MYSQL_PWD", dbPassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BackupException("mysqldump exited with code " + exitCode +
                        " — is mysqldump installed and on PATH?");
            }
        } catch (IOException | InterruptedException e) {
            throw new BackupException("Backup failed: " + e.getMessage(), e);
        }
    }

    private void runProcessWithFileInput(List<String> command, String dbPassword, Path sqlFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("MYSQL_PWD", dbPassword);
            pb.redirectInput(sqlFile.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BackupException("mysql restore exited with code " + exitCode +
                        " — is the mysql client installed and on PATH?");
            }
        } catch (IOException | InterruptedException e) {
            throw new BackupException("Restore failed: " + e.getMessage(), e);
        }
    }
}
