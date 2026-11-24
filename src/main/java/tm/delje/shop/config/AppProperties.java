package tm.delje.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String siteUrl;
    private String databaseUrl;
    private boolean cookieSecure;
    private String uploadsDir;
    private String backupsDir;
    private String importDir;
    private Admin admin = new Admin();
    private Mail mail = new Mail();
    private Backup backup = new Backup();

    /**
     * Site URL without the trailing slash, used for canonical links, hreflang and the sitemap.
     */
    public String getSiteUrl() {
        if (siteUrl != null && siteUrl.endsWith("/")) {
            return siteUrl.substring(0, siteUrl.length() - 1);
        }
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getUploadsDir() {
        return uploadsDir;
    }

    public void setUploadsDir(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    public String getBackupsDir() {
        return backupsDir;
    }

    public void setBackupsDir(String backupsDir) {
        this.backupsDir = backupsDir;
    }

    public String getImportDir() {
        return importDir;
    }

    public void setImportDir(String importDir) {
        this.importDir = importDir;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    public static class Admin {
        private String login;
        private String passwordHash;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        /**
         * The hash contains '$', so in .env it is written in single quotes (otherwise docker compose
         * tries to expand it). systemd removes the quotes itself, the local .env import does not.
         */
        public void setPasswordHash(String passwordHash) {
            String value = passwordHash == null ? null : passwordHash.trim();
            if (value != null && value.length() >= 2
                    && (value.startsWith("'") && value.endsWith("'") || value.startsWith("\"") && value.endsWith("\""))) {
                value = value.substring(1, value.length() - 1);
            }
            this.passwordHash = value;
        }
    }

    public static class Mail {
        private String resendApiUrl = "https://api.resend.com/emails";
        private String resendApiKey;
        private String adminEmail;
        private String from;

        public String getResendApiUrl() {
            return resendApiUrl;
        }

        public void setResendApiUrl(String resendApiUrl) {
            this.resendApiUrl = resendApiUrl;
        }

        public String getResendApiKey() {
            return resendApiKey;
        }

        public void setResendApiKey(String resendApiKey) {
            this.resendApiKey = resendApiKey;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Backup {
        private boolean enabled;
        private String cron;
        private String pgDumpPath;
        private int retentionDays = 14;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getPgDumpPath() {
            return pgDumpPath;
        }

        public void setPgDumpPath(String pgDumpPath) {
            this.pgDumpPath = pgDumpPath;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }
}
