package tm.delje.shop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tm.delje.shop.entity.Category;

class CatalogFilterTest {

    @Test
    void invalidValuesFallBackToDefaults() {
        CatalogFilter filter = CatalogFilter.fromRequest("/tk/products", null, null, "-5", "abc", "random", "x");
        assertNull(filter.getMin());
        assertNull(filter.getMax());
        assertEquals("new", filter.getSort());
        assertEquals(1, filter.getPage());
        assertFalse(filter.hasChips());
    }

    @Test
    void pageUrlKeepsFilters() {
        CatalogFilter filter = CatalogFilter.fromRequest("/ru/products", " туфли ", "obuv", "100.50", "1000", "cheap", "2");
        filter.setActiveCategory(category("obuv"));

        assertEquals("/ru/products?category=obuv&min=100.5&max=1000&q=%D1%82%D1%83%D1%84%D0%BB%D0%B8&sort=cheap&page=3",
                filter.pageUrl(3));
        assertEquals("/ru/products?category=obuv&min=100.5&max=1000&q=%D1%82%D1%83%D1%84%D0%BB%D0%B8&sort=cheap",
                filter.pageUrl(1));
    }

    @Test
    void chipUrlDropsOneParameter() {
        CatalogFilter filter = CatalogFilter.fromRequest("/tk/products", "", "obuv", "0", null, "new", "4");
        filter.setActiveCategory(category("obuv"));

        assertTrue(filter.hasChips());
        assertEquals("/tk/products?min=0", filter.urlWithout("category"));
        assertEquals("/tk/products?category=obuv", filter.urlWithout("min"));
    }

    @Test
    void unknownCategoryIsIgnored() {
        CatalogFilter filter = CatalogFilter.fromRequest("/tk/products", "", "missing", null, null, null, null);
        assertEquals("", filter.getCategory());
        assertEquals("/tk/products", filter.pageUrl(1));
    }

    private static Category category(String slug) {
        Category category = new Category();
        category.setId(1);
        category.setName("Обувь");
        category.setSlug(slug);
        return category;
    }
}
