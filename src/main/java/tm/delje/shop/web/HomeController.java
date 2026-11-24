package tm.delje.shop.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tm.delje.shop.entity.Banner;
import tm.delje.shop.entity.Category;
import tm.delje.shop.entity.Product;
import tm.delje.shop.service.CatalogService;

@Controller
public class HomeController {

    private final CatalogService catalogService;
    private final Messages messages;

    public HomeController(CatalogService catalogService, Messages messages) {
        this.catalogService = catalogService;
        this.messages = messages;
    }

    @GetMapping("/{lang:tk|ru}")
    public String home(@PathVariable String lang, Model model) {
        List<Banner> banners = catalogService.getActiveBanners();
        List<Category> categories = catalogService.getCategories();
        List<Product> topProducts = catalogService.getTopProducts();
        List<Product> newProducts = catalogService.getNewProducts();
        Map<Integer, Long> counts = catalogService.countProductsByCategory();

        List<Product> pool = new ArrayList<>(topProducts);
        pool.addAll(newProducts);

        // category tiles: product count and a cover taken from the products already loaded
        List<CategoryTile> categoryTiles = new ArrayList<>();
        for (Category category : categories) {
            String cover = pool.stream()
                    .filter(p -> category.getId().equals(p.getCategoryId()) && p.getMainImage() != null)
                    .map(p -> p.getMainImage().getThumb())
                    .findFirst()
                    .orElse(null);
            categoryTiles.add(new CategoryTile(category.getLocalizedName(lang), category.getSlug(),
                    counts.getOrDefault(category.getId(), 0L), cover));
        }

        List<HeroSlide> heroSlides = new ArrayList<>();
        for (Banner banner : banners) {
            heroSlides.add(HeroSlide.of(banner, lang));
        }

        // three product photos for the arch collage of the hero, the same on every slide
        List<Product> archSource = topProducts.isEmpty() ? newProducts : topProducts;
        List<String> heroArch = archSource.stream()
                .filter(p -> p.getMainImage() != null)
                .limit(3)
                .map(p -> p.getMainImage().getThumb())
                .toList();

        List<String> igTiles = pool.stream()
                .filter(p -> p.getMainImage() != null)
                .limit(4)
                .map(p -> p.getMainImage().getThumb())
                .toList();

        model.addAttribute("title", messages.get("meta.homeTitle", lang));
        model.addAttribute("metaDescription", messages.get("meta.homeDesc", lang));
        model.addAttribute("heroSlides", heroSlides);
        model.addAttribute("heroArch", heroArch);
        model.addAttribute("categoryTiles", categoryTiles);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("newProducts", newProducts);
        model.addAttribute("igTiles", igTiles);
        return "public/home";
    }

    public record CategoryTile(String name, String slug, long count, String cover) {
    }

    /**
     * One hero slide built from a banner. Eyebrow label and button text depend on where the banner links.
     */
    public record HeroSlide(String eyebrowKey, String title, String ctaKey, String href) {

        static HeroSlide of(Banner banner, String lang) {
            String link = banner.getLink() == null || banner.getLink().isEmpty() ? "/products" : banner.getLink();

            String eyebrowKey;
            if (link.contains("category=obuv")) {
                eyebrowKey = "home.eyebrow.shoes";
            } else if (link.contains("category=vechernie")) {
                eyebrowKey = "home.eyebrow.evening";
            } else if (link.contains("category=sumki")) {
                eyebrowKey = "home.eyebrow.bags";
            } else if (link.contains("/contact")) {
                eyebrowKey = "home.eyebrow.order";
            } else if (link.contains("sort=new")) {
                eyebrowKey = "home.eyebrow.new";
            } else {
                eyebrowKey = "home.eyebrow.collection";
            }
            String ctaKey = link.contains("/contact") ? "home.cta.contact" : "home.cta.view";

            String title = banner.getLocalizedTitle(lang);
            if (title.isEmpty()) {
                title = "DELJE";
            }

            // internal links get the language prefix
            String href = link;
            if (link.startsWith("/")) {
                href = "/".equals(link) ? "/" + lang : "/" + lang + link;
            }
            return new HeroSlide(eyebrowKey, title, ctaKey, href);
        }
    }
}
