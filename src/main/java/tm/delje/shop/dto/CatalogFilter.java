package tm.delje.shop.dto;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tm.delje.shop.entity.Category;

/**
 * Catalog query parameters. All filters live in the URL, so this class also builds
 * the links for filter chips and pagination.
 */
public class CatalogFilter {

    public static final List<String> SORT_VALUES = List.of("new", "old", "cheap", "expensive", "a-z", "z-a");

    private final String basePath;
    private String q = "";
    private String category = "";
    private BigDecimal min;
    private BigDecimal max;
    private String sort = "new";
    private int page = 1;
    private Category activeCategory;

    public CatalogFilter(String basePath) {
        this.basePath = basePath;
    }

    public static CatalogFilter fromRequest(String basePath, String q, String category, String min,
                                            String max, String sort, String page) {
        CatalogFilter filter = new CatalogFilter(basePath);
        filter.q = q == null ? "" : q.trim();
        filter.category = category == null ? "" : category.trim();
        filter.min = parseNonNegative(min);
        filter.max = parseNonNegative(max);
        filter.sort = sort != null && SORT_VALUES.contains(sort) ? sort : "new";
        filter.page = parsePage(page);
        return filter;
    }

    /**
     * Same filters without one parameter (link of a filter chip). Sorting is kept, page is reset.
     */
    public String urlWithout(String omit) {
        Map<String, String> params = new LinkedHashMap<>();
        if (!"category".equals(omit)) {
            params.put("category", getCategory());
        }
        if (!"min".equals(omit)) {
            params.put("min", getMinText());
        }
        if (!"max".equals(omit)) {
            params.put("max", getMaxText());
        }
        if (!"q".equals(omit)) {
            params.put("q", q);
        }
        if (!"new".equals(sort)) {
            params.put("sort", sort);
        }
        return buildUrl(params);
    }

    /**
     * Link to another page with all current filters.
     */
    public String pageUrl(int n) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("category", getCategory());
        params.put("min", getMinText());
        params.put("max", getMaxText());
        params.put("q", q);
        if (!"new".equals(sort)) {
            params.put("sort", sort);
        }
        if (n > 1) {
            params.put("page", String.valueOf(n));
        }
        return buildUrl(params);
    }

    public boolean hasChips() {
        return activeCategory != null || min != null || max != null || !q.isEmpty();
    }

    private String buildUrl(Map<String, String> params) {
        StringBuilder qs = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (!qs.isEmpty()) {
                qs.append('&');
            }
            qs.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return qs.isEmpty() ? basePath : basePath + "?" + qs;
    }

    private static BigDecimal parseNonNegative(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.signum() < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parsePage(String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    public String getQ() {
        return q;
    }

    /**
     * Category slug, only when such a category exists.
     */
    public String getCategory() {
        return activeCategory != null ? category : "";
    }

    public String getRequestedCategory() {
        return category;
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public String getMinText() {
        return plain(min);
    }

    public String getMaxText() {
        return plain(max);
    }

    public String getSort() {
        return sort;
    }

    public int getPage() {
        return page;
    }

    public Category getActiveCategory() {
        return activeCategory;
    }

    public void setActiveCategory(Category activeCategory) {
        this.activeCategory = activeCategory;
    }

    public String getBasePath() {
        return basePath;
    }
}
