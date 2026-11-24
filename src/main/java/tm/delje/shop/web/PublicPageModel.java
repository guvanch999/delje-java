package tm.delje.shop.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tm.delje.shop.config.AppProperties;

/**
 * Common model of every public page: language, links to the same page in the other
 * language, canonical URL and hreflang alternates.
 */
@Component
public class PublicPageModel {

    private final AppProperties properties;
    private final MessageSource messageSource;

    public PublicPageModel(AppProperties properties, MessageSource messageSource) {
        this.properties = properties;
        this.messageSource = messageSource;
    }

    public void fill(Map<String, Object> model, HttpServletRequest request, String lang) {
        String currentPath = request.getRequestURI();
        String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        // path without the /<lang> prefix, to build the same page in another language
        String basePath = currentPath;
        String prefix = "/" + lang;
        if (basePath.equals(prefix)) {
            basePath = "/";
        } else if (basePath.startsWith(prefix + "/")) {
            basePath = basePath.substring(prefix.length());
        }

        String origin = properties.getSiteUrl();
        Locale locale = Locale.forLanguageTag(lang);
        List<Hreflang> hreflangs = new ArrayList<>();
        List<LangLink> langLinks = new ArrayList<>();
        for (String code : Lang.SUPPORTED) {
            String href = langPath(code, basePath) + query;
            hreflangs.add(new Hreflang(code, origin + href));
            langLinks.add(new LangLink(code, message("lang." + code, locale), href, code.equals(lang)));
        }

        // menu: an item is active on its page and its sub-pages (catalog stays active on product pages)
        String home = "/" + lang;
        List<NavItem> navItems = new ArrayList<>();
        navItems.add(new NavItem(home, message("nav.home", locale), currentPath.equals(home)));
        for (String[] item : new String[][] {{"/products", "nav.catalog"}, {"/about", "nav.about"}, {"/contact", "nav.contact"}}) {
            String href = home + item[0];
            boolean active = currentPath.equals(href) || currentPath.startsWith(href + "/");
            navItems.add(new NavItem(href, message(item[1], locale), active));
        }

        model.put("lang", lang);
        model.put("navItems", navItems);
        model.put("isHome", currentPath.equals(home));
        model.put("currentPath", currentPath);
        model.put("canonicalUrl", origin + currentPath + query);
        model.put("hreflangs", hreflangs);
        model.put("langLinks", langLinks);
        model.put("siteUrl", origin);
    }

    private static String langPath(String lang, String basePath) {
        return "/".equals(basePath) ? "/" + lang : "/" + lang + basePath;
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    public record NavItem(String href, String label, boolean active) {
    }

    public record Hreflang(String lang, String href) {
    }

    public record LangLink(String code, String label, String href, boolean active) {
    }
}
