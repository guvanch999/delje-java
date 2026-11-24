package tm.delje.shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily database backup, enabled with BACKUP_ENABLED=true (on the production server).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.backup.enabled", havingValue = "true")
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;
    private final String cron;

    public BackupScheduler(BackupService backupService, @Value("${app.backup.cron}") String cron) {
        this.backupService = backupService;
        this.cron = cron;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSchedule() {
        log.info("daily database backups scheduled, cron: {}", cron);
    }

    @Scheduled(cron = "${app.backup.cron}")
    public void backup() {
        backupService.runBackup();
    }
}
