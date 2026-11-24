package tm.delje.shop.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tm.delje.shop.dto.CatalogFilter;
import tm.delje.shop.entity.Category;
import tm.delje.shop.entity.Product;
import tm.delje.shop.service.CatalogService;
import tm.delje.shop.service.NotFoundException;

@Controller
public class CatalogController {

    private final CatalogService catalogService;
    private final Messages messages;

    public CatalogController(CatalogService catalogService, Messages messages) {
        this.catalogService = catalogService;
        this.messages = messages;
    }

    @GetMapping("/{lang:tk|ru}/products")
    public String catalog(@PathVariable String lang,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String min,
                          @RequestParam(required = false) String max,
                          @RequestParam(required = false) String sort,
                          @RequestParam(required = false) String page,
                          Model model) {
        CatalogFilter filters = CatalogFilter.fromRequest("/" + lang + "/products", q, category, min, max, sort, page);

        List<Category> categories = catalogService.getCategories();
        if (!filters.getRequestedCategory().isEmpty()) {
            categories.stream()
                    .filter(c -> c.getSlug().equals(filters.getRequestedCategory()))
                    .findFirst()
                    .ifPresent(filters::setActiveCategory);
        }

        Integer categoryId = filters.getActiveCategory() != null ? filters.getActiveCategory().getId() : null;
        Page<Product> result = catalogService.search(categoryId, filters.getQ(), filters.getMin(), filters.getMax(),
                filters.getSort(), filters.getPage());
        int totalPages = Math.max(1, result.getTotalPages());

        model.addAttribute("title", messages.get("meta.catalogTitle", lang));
        model.addAttribute("metaDescription", messages.get("meta.catalogDesc", lang));
        model.addAttribute("items", result.getContent());
        model.addAttribute("count", result.getTotalElements());
        model.addAttribute("page", filters.getPage());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("categories", categories);
        model.addAttribute("filters", filters);
        model.addAttribute("sortOptions", CatalogFilter.SORT_VALUES);
        return "public/products";
    }

    @GetMapping("/{lang:tk|ru}/products/{slug}")
    public String product(@PathVariable String lang, @PathVariable String slug, Model model) {
        Product product = catalogService.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("product " + slug));
        List<Product> related = catalogService.getRelated(product);

        String name = product.getLocalizedName(lang);
        String description = product.getLocalizedDescription(lang);
        String metaDescription = description.length() > 160 ? description.substring(0, 160) : description;
        if (metaDescription.isEmpty()) {
            metaDescription = name + " " + messages.get("meta.productFallback", lang);
        }

        model.addAttribute("title", name);
        model.addAttribute("metaDescription", metaDescription);
        model.addAttribute("product", product);
        model.addAttribute("productName", name);
        model.addAttribute("productDescription", description);
        model.addAttribute("related", related);
        return "public/product";
    }
}
