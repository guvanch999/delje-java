package tm.delje.shop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tm.delje.shop.entity.Localized;

class FormatTest {

    @Test
    void slugTransliteratesCyrillic() {
        assertEquals("kozhanye-bosonozhki-s-peryami", SlugUtils.slugify("Кожаные босоножки с перьями"));
        assertEquals("obuv", SlugUtils.slugify("Обувь"));
        assertEquals("vechernie-sumki", SlugUtils.slugify("Вечерние сумки"));
        assertEquals("krossovki-air-max-90", SlugUtils.slugify("Кроссовки Air Max 90"));
        assertEquals("item", SlugUtils.slugify("!!!"));
        assertEquals("item", SlugUtils.slugify(null));
    }

    @Test
    void uniqueSlugAddsNumber() {
        assertEquals("obuv-3", SlugUtils.uniqueSlug("Обувь", s -> s.equals("obuv") || s.equals("obuv-2")));
        assertEquals("obuv", SlugUtils.uniqueSlug("Обувь", s -> false));
    }

    @Test
    void priceUsesRussianFormat() {
        assertEquals("12 500 TMT", PriceFormat.tmt(new BigDecimal("12500.00")));
        assertEquals("890 TMT", PriceFormat.tmt(new BigDecimal("890.00")));
        assertEquals("990,5 TMT", PriceFormat.tmt(new BigDecimal("990.50")));
        assertEquals("1 234 567,89 ₽", PriceFormat.rub(new BigDecimal("1234567.89")));
    }

    @Test
    void russianPlurals() {
        assertEquals("1 товар", CountFormat.products(1, "ru"));
        assertEquals("3 товара", CountFormat.products(3, "ru"));
        assertEquals("5 товаров", CountFormat.products(5, "ru"));
        assertEquals("11 товаров", CountFormat.products(11, "ru"));
        assertEquals("21 товар", CountFormat.products(21, "ru"));
        assertEquals("112 товаров", CountFormat.products(112, "ru"));
        assertEquals("47 haryt", CountFormat.products(47, "tk"));
    }

    @Test
    void localizedFallsBackToOtherLanguage() {
        assertEquals("Aýakgap", Localized.pick("Обувь", "Aýakgap", "tk"));
        assertEquals("Обувь", Localized.pick("Обувь", "Aýakgap", "ru"));
        assertEquals("Обувь", Localized.pick("Обувь", null, "tk"));
        assertEquals("Обувь", Localized.pick("Обувь", "", "tk"));
        assertEquals("Aýakgap", Localized.pick(null, "Aýakgap", "ru"));
        assertEquals("", Localized.pick(null, null, "ru"));
    }
}
