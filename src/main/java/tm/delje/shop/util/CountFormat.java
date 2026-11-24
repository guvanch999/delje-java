package tm.delje.shop.util;

public final class CountFormat {

    private CountFormat() {
    }

    /**
     * "N products" in the page language. Russian needs plural forms,
     * in Turkmen the noun does not change after a number.
     */
    public static String products(long n, String lang) {
        if ("ru".equals(lang)) {
            long mod10 = n % 10;
            long mod100 = n % 100;
            String word;
            if (mod10 == 1 && mod100 != 11) {
                word = "товар";
            } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
                word = "товара";
            } else {
                word = "товаров";
            }
            return n + " " + word;
        }
        return n + " haryt";
    }
}
