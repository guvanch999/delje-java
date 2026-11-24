package tm.delje.shop.util;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public final class SlugUtils {

    private static final Map<Character, String> TRANSLIT = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""),
            Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya"));

    private SlugUtils() {
    }

    /**
     * Readable URL part from a name: Cyrillic is transliterated, everything except
     * latin letters and digits becomes a dash. Falls back to "item" for an empty result.
     */
    public static String slugify(String text) {
        if (text == null) {
            return "item";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (char c : lower.toCharArray()) {
            String replacement = TRANSLIT.get(c);
            if (replacement != null) {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        String slug = sb.toString()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "item" : slug;
    }

    /**
     * Adds -2, -3, ... to the slug until {@code taken} says it is free.
     */
    public static String uniqueSlug(String name, Predicate<String> taken) {
        String base = slugify(name);
        String candidate = base;
        int i = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }
}
