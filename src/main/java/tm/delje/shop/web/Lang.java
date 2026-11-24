package tm.delje.shop.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

/**
 * Languages of the public site. Turkmen is the main one, Russian the second.
 */
public final class Lang {

    public static final String DEFAULT = "tk";
    public static final List<String> SUPPORTED = List.of("tk", "ru");
    public static final String COOKIE_NAME = "lang";

    private Lang() {
    }

    public static boolean isSupported(String lang) {
        return lang != null && SUPPORTED.contains(lang);
    }

    /**
     * Language prefix of the path ("/ru/products" gives "ru"), or null.
     */
    public static String fromPath(String path) {
        if (path == null) {
            return null;
        }
        String[] parts = path.split("/");
        if (parts.length > 1 && isSupported(parts[1])) {
            return parts[1];
        }
        return null;
    }

    /**
     * Language for requests outside the /tk and /ru sections (root redirect, 404 pages):
     * path prefix, then the saved choice, then Accept-Language, then the default.
     */
    public static String resolve(HttpServletRequest request) {
        String fromPath = fromPath(request.getRequestURI());
        if (fromPath != null) {
            return fromPath;
        }

        String saved = savedLang(request);
        if (isSupported(saved)) {
            return saved;
        }

        String header = request.getHeader("Accept-Language");
        if (header != null) {
            for (String part : header.split(",")) {
                String code = part.trim();
                if (code.length() >= 2) {
                    code = code.substring(0, 2).toLowerCase(Locale.ROOT);
                    if (isSupported(code)) {
                        return code;
                    }
                }
            }
        }
        return DEFAULT;
    }

    public static String savedLang(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
