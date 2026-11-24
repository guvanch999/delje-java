package tm.delje.shop.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Prices are written the Russian way: non-breaking space between thousands,
 * comma before decimals, no trailing zeros ("12 500", "990,5").
 */
public final class PriceFormat {

    private PriceFormat() {
    }

    public static String number(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        symbols.setMinusSign('-');
        DecimalFormat format = new DecimalFormat("#,##0.###", symbols);
        return format.format(value);
    }

    public static String tmt(BigDecimal value) {
        return number(value) + " TMT";
    }

    public static String rub(BigDecimal value) {
        return number(value) + " ₽";
    }
}
