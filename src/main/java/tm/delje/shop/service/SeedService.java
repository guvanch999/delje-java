package tm.delje.shop.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tm.delje.shop.entity.Banner;
import tm.delje.shop.entity.Category;
import tm.delje.shop.entity.Product;
import tm.delje.shop.entity.ProductImage;
import tm.delje.shop.repository.BannerRepository;
import tm.delje.shop.repository.CategoryRepository;
import tm.delje.shop.repository.ProductRepository;
import tm.delje.shop.util.SlugUtils;

/**
 * Test data for development: generic categories and products with plain colour placeholder pictures.
 * All existing catalog data is removed first.
 */
@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private static final Color[] COLORS = {
            new Color(99, 102, 241), new Color(16, 185, 129), new Color(239, 68, 68), new Color(245, 158, 11),
            new Color(14, 165, 233), new Color(168, 85, 247), new Color(236, 72, 153), new Color(34, 197, 94)
    };

    private static final String[][] CATEGORIES = {
            {"Кроссовки", "Krossowkalar"},
            {"Одежда", "Egin-eşik"},
            {"Аксессуары", "Aksesuarlar"},
            {"Рюкзаки", "Ryukzaklar"},
            {"Мячи", "Toplar"},
            {"Головные уборы", "Kelle geýimleri"}
    };

    private static final String[] SNEAKERS = {
            "Кроссовки Air Max 90", "Кроссовки Air Force 1", "Кроссовки Air Max 270",
            "Кроссовки Pegasus 40", "Кроссовки Dunk Low", "Кроссовки Blazer Mid",
            "Кроссовки Ultraboost 22", "Кроссовки Superstar", "Кроссовки Stan Smith",
            "Кроссовки Gazelle", "Кроссовки Samba OG", "Кроссовки NMD R1",
            "Кроссовки Suede Classic", "Кроссовки RS-X", "Кроссовки Future Rider",
            "Кроссовки 574", "Кроссовки 990v5", "Кроссовки 327",
            "Кроссовки Classic Leather", "Кроссовки Club C 85", "Кроссовки Nano X3"
    };

    // category, name, min price, max price
    private static final Object[][] OTHER_PRODUCTS = {
            {"Одежда", "Футболка Dri-FIT", 1500, 4500},
            {"Одежда", "Худи Essentials", 3500, 8500},
            {"Одежда", "Шорты Training", 1800, 4200},
            {"Одежда", "Ветровка Windrunner", 4500, 11000},
            {"Одежда", "Спортивные брюки", 3000, 7500},
            {"Аксессуары", "Носки Everyday (3 пары)", 800, 2200},
            {"Аксессуары", "Перчатки для бега", 1200, 3200},
            {"Аксессуары", "Бутылка для воды", 900, 2500},
            {"Рюкзаки", "Рюкзак Brasilia", 3200, 7500},
            {"Рюкзаки", "Рюкзак Power", 3800, 8900},
            {"Мячи", "Футбольный мяч Strike", 2200, 5500},
            {"Мячи", "Баскетбольный мяч Elite", 2800, 6200},
            {"Головные уборы", "Кепка Heritage 86", 1500, 3500},
            {"Головные уборы", "Шапка бини", 1300, 2900}
    };

    private final ImageService imageService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BannerRepository bannerRepository;
    private final JdbcTemplate jdbcTemplate;

    public SeedService(ImageService imageService, CategoryRepository categoryRepository,
                       ProductRepository productRepository, BannerRepository bannerRepository,
                       JdbcTemplate jdbcTemplate) {
        this.imageService = imageService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.bannerRepository = bannerRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void seed() throws IOException {
        generatePlaceholders();
        DataImportService.truncateCatalog(jdbcTemplate);

        Map<String, Category> categories = new HashMap<>();
        Set<String> categorySlugs = new HashSet<>();
        for (String[] row : CATEGORIES) {
            Category category = new Category();
            category.setName(row[0]);
            category.setNameTk(row[1]);
            category.setSlug(DataImportService.uniqueInSet(categorySlugs, SlugUtils.slugify(row[0])));
            categories.put(row[0], categoryRepository.save(category));
        }

        Set<String> productSlugs = new HashSet<>();
        List<Product> products = new ArrayList<>();
        for (String name : SNEAKERS) {
            products.add(newProduct(name, name + " — технологичная и удобная пара для города и спорта.",
                    random(40, 250) * 100, categories.get("Кроссовки"), productSlugs));
        }
        for (Object[] row : OTHER_PRODUCTS) {
            String name = (String) row[1];
            products.add(newProduct(name, name + ". Качественный материал и современный дизайн.",
                    random((Integer) row[2], (Integer) row[3]), categories.get((String) row[0]), productSlugs));
        }

        int topCount = 8;
        Set<Integer> topIndexes = new HashSet<>();
        while (topIndexes.size() < topCount) {
            topIndexes.add(random(0, products.size() - 1));
        }
        for (Integer i : topIndexes) {
            products.get(i).setTop(true);
        }

        int imageCount = 0;
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            int count = random(1, 3);
            for (int s = 0; s < count; s++) {
                int n = (i + s) % COLORS.length + 1;
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setThumb("/uploads/products/placeholder-" + n + ".webp");
                image.setFull("/uploads/products/placeholder-" + n + "-full.webp");
                image.setSort(s);
                product.getImages().add(image);
                imageCount++;
            }
            productRepository.save(product);
        }

        String[][] banners = {
                {"Новая коллекция", "Täze kolleksiýa"},
                {"Скидки до 40%", "Arzanladyş 40%-e çenli"},
                {"Топ кроссовки сезона", "Möwsümiň iň gowy krossowkalary"}
        };
        for (int i = 0; i < banners.length; i++) {
            Banner banner = new Banner();
            banner.setImage("/uploads/products/banner-" + (i + 1) + ".webp");
            banner.setTitle(banners[i][0]);
            banner.setTitleTk(banners[i][1]);
            banner.setLink("/products");
            banner.setActive(true);
            bannerRepository.save(banner);
        }

        log.info("seed finished: {} categories, {} products ({} top), {} images, 3 banners",
                categories.size(), products.size(), topCount, imageCount);
    }

    private Product newProduct(String name, String description, int price, Category category, Set<String> slugs) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(DataImportService.uniqueInSet(slugs, SlugUtils.slugify(name)));
        product.setDescription(description);
        product.setPrice(BigDecimal.valueOf(price));
        product.setCategory(category);
        return product;
    }

    private void generatePlaceholders() throws IOException {
        for (int i = 0; i < COLORS.length; i++) {
            int n = i + 1;
            imageService.saveGenerated(solid(400, 400, COLORS[i]), "placeholder-" + n + ".webp");
            imageService.saveGenerated(solid(1600, 1200, COLORS[i]), "placeholder-" + n + "-full.webp");
        }
        for (int n = 1; n <= 3; n++) {
            imageService.saveGenerated(solid(1920, 600, COLORS[n]), "banner-" + n + ".webp");
        }
    }

    private static BufferedImage solid(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
