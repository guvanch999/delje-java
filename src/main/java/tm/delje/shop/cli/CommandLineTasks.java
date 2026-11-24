package tm.delje.shop.cli;

import java.util.Set;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tm.delje.shop.DeljeApplication;
import tm.delje.shop.service.BackupService;
import tm.delje.shop.service.DataImportService;
import tm.delje.shop.service.SeedService;

/**
 * Maintenance tasks started from the same jar, without the web server:
 * <pre>
 *   java -jar delje.jar import            load the DELJE catalog from import/
 *   java -jar delje.jar seed              fill the database with test data
 *   java -jar delje.jar backup            make a database dump now
 *   java -jar delje.jar hash-password pw  print a bcrypt hash for ADMIN_PASSWORD_HASH
 * </pre>
 */
public final class CommandLineTasks {

    private static final Set<String> TASKS = Set.of("import", "seed", "backup", "hash-password");

    private CommandLineTasks() {
    }

    public static boolean isTask(String name) {
        return TASKS.contains(name);
    }

    public static int run(String[] args) {
        String task = args[0];

        if ("hash-password".equals(task)) {
            if (args.length < 2 || args[1].isEmpty()) {
                System.err.println("Usage: java -jar delje.jar hash-password <password>");
                return 1;
            }
            System.out.println(new BCryptPasswordEncoder(12).encode(args[1]));
            return 0;
        }

        // passed as a command line argument so it wins over BACKUP_ENABLED from .env
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DeljeApplication.class)
                .web(WebApplicationType.NONE)
                .run("--app.backup.enabled=false")) {
            switch (task) {
                case "import" -> context.getBean(DataImportService.class).importCatalog();
                case "seed" -> context.getBean(SeedService.class).seed();
                case "backup" -> {
                    if (!context.getBean(BackupService.class).runBackup()) {
                        return 1;
                    }
                }
                default -> {
                    return 1;
                }
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Task '" + task + "' failed: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
