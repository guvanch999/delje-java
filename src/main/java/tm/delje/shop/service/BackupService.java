package tm.delje.shop.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tm.delje.shop.config.AppProperties;
import tm.delje.shop.config.DatabaseUrl;

/**
 * Database backups: pg_dump in custom format to backups/db_YYYY-MM-DD.dump,
 * then removal of dumps older than the retention period.
 * Errors are logged and never thrown, a failed backup must not affect the site.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final AppProperties properties;

    public BackupService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * @return true when the dump was created
     */
    public boolean runBackup() {
        DatabaseUrl db;
        try {
            db = DatabaseUrl.parse(properties.getDatabaseUrl());
        } catch (IllegalArgumentException e) {
            log.error("backup skipped: {}", e.getMessage());
            return false;
        }

        Path dir = backupsDir();
        String date = LocalDate.now(ZoneOffset.UTC).toString();
        Path file = dir.resolve("db_" + date + ".dump");

        try {
            Files.createDirectories(dir);

            List<String> command = new ArrayList<>();
            command.add(properties.getBackup().getPgDumpPath());
            command.add("--host=" + db.host());
            command.add("--port=" + db.port());
            command.add("--dbname=" + db.database());
            if (db.username() != null) {
                command.add("--username=" + db.username());
            }
            command.add("--format=custom");
            command.add("--no-password");
            command.add("--file=" + file);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            // password goes through the environment, not the command line
            if (db.password() != null) {
                builder.environment().put("PGPASSWORD", db.password());
            }

            Process process = builder.start();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(30, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                log.error("backup failed: pg_dump did not finish in 30 minutes");
                return false;
            }
            if (process.exitValue() != 0) {
                log.error("backup failed: pg_dump exited with {}: {}", process.exitValue(),
                        new String(output, StandardCharsets.UTF_8).trim());
                return false;
            }
            log.info("backup created: {}", file);
        } catch (IOException e) {
            log.error("backup failed", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("backup interrupted");
            return false;
        }

        rotateOldBackups();
        return true;
    }

    /**
     * Deletes db_*.dump files older than the retention period.
     */
    public void rotateOldBackups() {
        Path dir = backupsDir();
        Instant border = Instant.now().minus(properties.getBackup().getRetentionDays(), ChronoUnit.DAYS);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "db_*.dump")) {
            for (Path file : files) {
                try {
                    if (Files.getLastModifiedTime(file).toInstant().isBefore(border)) {
                        Files.delete(file);
                        log.info("old backup removed: {}", file);
                    }
                } catch (IOException e) {
                    log.error("backup rotation: cannot process {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("backup rotation: cannot read {}", dir, e);
        }
    }

    private Path backupsDir() {
        return Paths.get(properties.getBackupsDir()).toAbsolutePath().normalize();
    }
}
