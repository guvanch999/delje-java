package tm.delje.shop.web;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import tm.delje.shop.util.CountFormat;
import tm.delje.shop.util.PriceFormat;

/**
 * Formatting helpers for templates: {@code ${@fmt.price(product.price)}}.
 */
@Component("fmt")
public class ViewFormat {

    public String price(BigDecimal value) {
        return PriceFormat.tmt(value);
    }

    public String adminPrice(BigDecimal value) {
        return PriceFormat.rub(value);
    }

    public String count(long n, String lang) {
        return CountFormat.products(n, lang);
    }
}
