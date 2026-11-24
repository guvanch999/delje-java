package tm.delje.shop.web;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Short access to UI texts from Java code (page titles, meta descriptions).
 */
@Component
public class Messages {

    private final MessageSource messageSource;

    public Messages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key, String lang) {
        return messageSource.getMessage(key, null, key, Locale.forLanguageTag(lang));
    }
}
