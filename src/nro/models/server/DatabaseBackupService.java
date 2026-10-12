package nro.models.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.models.utils.Logger;

/** Creates a rolling SQL backup and keeps only the latest successful dump. */
public final class DatabaseBackupService {

    private static final long BACKUP_INTERVAL_MINUTES = 30L;
    private static final Path BACKUP_DIRECTORY = Paths.get("backups", "database");
    private static final String BACKUP_PREFIX = "database-";
    private static final String BACKUP_SUFFIX = ".sql";

    private static DatabaseBackupService instance;
    private ScheduledExecutorService scheduler;

    private DatabaseBackupService() {
    }

    public static synchronized DatabaseBackupService gI() {
        if (instance == null) {
            instance = new DatabaseBackupService();
        }
        return instance;
    }

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Database backup");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::backupSafely,
                BACKUP_INTERVAL_MINUTES, BACKUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
        Logger.success("AUTO BACKUP SQL EVERY 30 MINUTES\n");
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void backupSafely() {
        try {
            createBackup();
        } catch (Exception e) {
            Logger.logException(DatabaseBackupService.class, e);
        }
    }

    private void createBackup() throws Exception {
        Properties config = loadConfig();
        String host = config.getProperty("database.host", "localhost");
        String port = config.getProperty("database.port", "3306");
        String database = config.getProperty("database.name", "yeupich");
        String user = config.getProperty("database.user", "root");
        String password = config.getProperty("database.pass", "");
        String dumpExecutable = findDumpExecutable(config.getProperty("database.backup.mysqldump", ""));

        Files.createDirectories(BACKUP_DIRECTORY);
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path temporary = BACKUP_DIRECTORY.resolve(BACKUP_PREFIX + timestamp + ".tmp");
        Path completed = BACKUP_DIRECTORY.resolve(BACKUP_PREFIX + timestamp + BACKUP_SUFFIX);

        List<String> command = new ArrayList<>();
        command.add(dumpExecutable);
        command.add("--single-transaction");
        command.add("--quick");
        // Quarantine the corrupt website table; reading its metadata crashes MariaDB.
        command.add("--ignore-table=" + database + ".site_comments");
        command.add("--skip-lock-tables");
        command.add("--default-character-set=utf8mb4");
        command.add("-h");
        command.add(host);
        command.add("-P");
        command.add(port);
        command.add("-u");
        command.add(user);
        command.add(database);

        ProcessBuilder builder = new ProcessBuilder(command);
        if (!password.isEmpty()) {
            builder.environment().put("MYSQL_PWD", password);
        }
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(temporary.toFile());

        int exitCode;
        try {
            exitCode = builder.start().waitFor();
        } catch (Exception e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
        if (exitCode != 0 || !Files.exists(temporary) || Files.size(temporary) == 0) {
            Files.deleteIfExists(temporary);
            throw new IOException("mysqldump failed with exit code " + exitCode);
        }

        Files.move(temporary, completed, StandardCopyOption.REPLACE_EXISTING);
        deleteOlderBackups(completed);
        Logger.success("[SQL Backup] Saved: " + completed.toAbsolutePath() + "\n");
    }

    private Properties loadConfig() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("Config.properties")) {
            properties.load(input);
        }
        return properties;
    }

    private String findDumpExecutable(String configuredPath) {
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            return configuredPath.trim();
        }
        String[] commonPaths = {
            "C:\\xampp\\mysql\\bin\\mysqldump.exe",
            "C:\\laragon\\bin\\mysql\\mysql-8.0.30-winx64\\bin\\mysqldump.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe"
        };
        for (String path : commonPaths) {
            if (new File(path).isFile()) {
                return path;
            }
        }
        return "mysqldump";
    }

    private void deleteOlderBackups(Path newest) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                BACKUP_DIRECTORY, BACKUP_PREFIX + "*" + BACKUP_SUFFIX)) {
            for (Path backup : stream) {
                if (!backup.equals(newest)) {
                    Files.deleteIfExists(backup);
                }
            }
        }
    }
}
