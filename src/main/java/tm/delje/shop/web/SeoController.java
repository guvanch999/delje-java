package tm.delje.shop.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.HtmlUtils;
import tm.delje.shop.config.AppProperties;
import tm.delje.shop.repository.ProductRepository;

/**
 * sitemap.xml (every page in both languages with hreflang alternates) and robots.txt.
 */
@Controller
public class SeoController {

    private static final List<String> STATIC_PATHS = List.of("/", "/products", "/about", "/contact");

    private final ProductRepository productRepository;
    private final AppProperties properties;

    public SeoController(ProductRepository productRepository, AppProperties properties) {
        this.productRepository = productRepository;
        this.properties = properties;
    }

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        String base = properties.getSiteUrl();
        List<String> paths = new ArrayList<>(STATIC_PATHS);
        for (String slug : productRepository.findAllSlugs()) {
            paths.add("/products/" + slug);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" ")
                .append("xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n");
        for (String path : paths) {
            for (String lang : Lang.SUPPORTED) {
                xml.append("  <url>\n");
                xml.append("    <loc>").append(langUrl(base, lang, path)).append("</loc>\n");
                for (String alt : Lang.SUPPORTED) {
                    xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"").append(alt)
                            .append("\" href=\"").append(langUrl(base, alt, path)).append("\" />\n");
                }
                xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"")
                        .append(langUrl(base, Lang.DEFAULT, path)).append("\" />\n");
                xml.append("  </url>\n");
            }
        }
        xml.append("</urlset>\n");

        return ResponseEntity.ok()
                .contentType(new MediaType("application", "xml", java.nio.charset.StandardCharsets.UTF_8))
                .body(xml.toString());
    }

    @GetMapping("/robots.txt")
    public ResponseEntity<String> robots() {
        String body = "User-agent: *\n"
                + "Allow: /\n"
                + "Disallow: /admin\n"
                + "Sitemap: " + properties.getSiteUrl() + "/sitemap.xml\n";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(body);
    }

    private static String langUrl(String base, String lang, String path) {
        String url = "/".equals(path) ? base + "/" + lang : base + "/" + lang + path;
        return HtmlUtils.htmlEscape(url);
    }
}
