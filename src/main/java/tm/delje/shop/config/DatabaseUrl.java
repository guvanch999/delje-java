package tm.delje.shop.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Parsed form of DATABASE_URL (postgres://user:password@host:port/database?params).
 * The same value is used for the JDBC pool and for pg_dump.
 */
public record DatabaseUrl(String host, int port, String database, String username, String password, String query) {

    public static DatabaseUrl parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is not set");
        }
        String value = url.trim();
        if (value.startsWith("jdbc:")) {
            value = value.substring("jdbc:".length());
        }
        if (!value.startsWith("postgres://") && !value.startsWith("postgresql://")) {
            throw new IllegalArgumentException("DATABASE_URL must start with postgres:// or postgresql://");
        }

        URI uri = URI.create(value);
        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = decode(userInfo.substring(0, colon));
                password = decode(userInfo.substring(colon + 1));
            } else {
                username = decode(userInfo);
            }
        }

        String path = uri.getPath();
        String database = path != null && path.length() > 1 ? decode(path.substring(1)) : "postgres";
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        return new DatabaseUrl(uri.getHost(), port, database, username, password, uri.getRawQuery());
    }

    public String jdbcUrl() {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        if (query != null && !query.isBlank()) {
            url += "?" + query;
        }
        return url;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
