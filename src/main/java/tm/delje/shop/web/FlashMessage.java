package tm.delje.shop.web;

import java.io.Serializable;

/**
 * Message shown once on the next admin page after a redirect.
 */
public record FlashMessage(String type, String text) implements Serializable {

    public static FlashMessage success(String text) {
        return new FlashMessage("success", text);
    }

    public static FlashMessage error(String text) {
        return new FlashMessage("error", text);
    }
}
