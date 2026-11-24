package tm.delje.shop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Locale for message lookups comes from the URL prefix (/tk, /ru).
 * The chosen language is remembered by {@link PublicPageInterceptor}.
 */
public class LangLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return Locale.forLanguageTag(Lang.resolve(request));
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("language is selected by the URL prefix");
    }
}
