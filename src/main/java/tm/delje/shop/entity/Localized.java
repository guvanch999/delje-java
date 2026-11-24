package tm.delje.shop.entity;

/**
 * Text columns are stored twice: the base column is Russian, the *_tk column is Turkmen.
 * The showcase falls back to the other language when a translation is empty.
 */
public final class Localized {

    private Localized() {
    }

    public static String pick(String ru, String tk, String lang) {
        String value;
        if ("tk".equals(lang)) {
            value = isEmpty(tk) ? ru : tk;
        } else {
            value = isEmpty(ru) ? tk : ru;
        }
        return value == null ? "" : value;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
