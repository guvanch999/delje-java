package tm.delje.shop.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tm.delje.shop.config.AppProperties;
import tm.delje.shop.entity.Banner;
import tm.delje.shop.entity.Category;
import tm.delje.shop.entity.Product;
import tm.delje.shop.entity.ProductImage;
import tm.delje.shop.repository.BannerRepository;
import tm.delje.shop.repository.CategoryRepository;
import tm.delje.shop.repository.ProductRepository;
import tm.delje.shop.util.SlugUtils;

/**
 * Loads the real DELJE catalog from the import/ folder: 3 categories, product photos
 * (tovar_NN*.png) and web banners. All existing catalog data is removed first.
 */
@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private record Item(int photo, String name, String nameTk, int price, boolean top) {
    }

    private record CatalogSection(String name, String nameTk, List<Item> items,
                                  Function<String, String> description, Function<String, String> descriptionTk) {
    }

    private record BannerItem(String file, String title, String titleTk, String link) {
    }

    private static final List<Item> SHOES = List.of(
            new Item(1, "Кожаные босоножки с перьями", "Ýelekli deri sandallar", 890, true),
            new Item(2, "Текстильные кеды с вышивкой", "Keşdeli dokma kedler", 650, false),
            new Item(3, "Кожаные балетки с квадратным носком", "Inedördül burunly deri baletkalar", 520, false),
            new Item(9, "Атласные босоножки с розой, лаймовые", "Bägülli atlas sandallar, laým reňk", 720, false),
            new Item(11, "Слингбэки с Т-образным ремешком", "T-şekilli gaýyşly slingbekler", 680, false),
            new Item(13, "Ажурные слингбэки, айвори", "Nagyşly slingbekler, aýwori", 750, true),
            new Item(14, "Мюли со стразами, золотистые", "Strazly mýuli, altyn reňk", 830, false),
            new Item(23, "Атласные босоножки с розой, серебристые", "Bägülli atlas sandallar, kümüş reňk", 720, false),
            new Item(24, "Атласные босоножки с розой, розовые", "Bägülli atlas sandallar, gülgüne", 720, false),
            new Item(33, "Атласные туфли с кристаллическим бантом", "Kristally bantly atlas köwüşler", 780, false),
            new Item(34, "Туфли с фатиновым бантом, нюд", "Fatin bantly köwüşler, nýud", 850, true),
            new Item(35, "Мюли с кристаллами, белые", "Kristally mýuli, ak", 890, false),
            new Item(36, "Слингбэки с кристаллами, нюд", "Kristally slingbekler, nýud", 860, false),
            new Item(47, "Слингбэки с бантом, розовые", "Bantly slingbekler, gülgüne", 790, false),
            new Item(48, "Босоножки на платформе с кристаллом", "Kristally platforma sandallar", 940, true),
            new Item(52, "Мюли из сетки с бантом, айвори", "Bantly tor mýuli, aýwori", 690, false),
            new Item(53, "Кроссовки со стразами, чёрные", "Strazly krossowkalar, gara", 990, true));

    private static final List<Item> BAGS = List.of(
            new Item(4, "Тканевая сумка-тоут, бежевая", "Dokma tout-sumka, bež", 780, false),
            new Item(5, "Тканевая сумка-тоут, чёрная", "Dokma tout-sumka, gara", 780, false),
            new Item(6, "Стёганая мини-сумка на цепочке", "Zynjyrly stežkaly mini-sumka", 690, false),
            new Item(7, "Замшевая стёганая мини-сумка", "Zamşa stežkaly mini-sumka", 720, false),
            new Item(8, "Тканевая сумка-шоппер, бежевая", "Dokma şopper-sumka, bež", 850, false),
            new Item(12, "Лаковая сумка с верхней ручкой", "Ýokary tutawaçly lak sumka", 990, true),
            new Item(17, "Кожаная сумка с пряжкой, чёрная", "Tokaly deri sumka, gara", 1150, false),
            new Item(18, "Джинсовая сумка с чёрной отделкой", "Gara bezegli jinsi sumka", 1050, false),
            new Item(19, "Джинсовая сумка с коричневой отделкой", "Goňur bezegli jinsi sumka", 1050, false),
            new Item(20, "Замшевая сумка-тоут, коричневая", "Zamşa tout-sumka, goňur", 1290, true),
            new Item(21, "Замшевая сумка с ремнём", "Gaýyşly zamşa sumka", 1190, false),
            new Item(22, "Замшевая сумка-полумесяц", "Ýarymaý şekilli zamşa sumka", 1090, false),
            new Item(25, "Кожаная сумка с бантами, айвори", "Bantly deri sumka, aýwori", 990, false),
            new Item(26, "Кожаная сумка с бантом, розовая", "Bantly deri sumka, gülgüne", 990, false),
            new Item(27, "Кожаная сумка с бантом, чёрная", "Bantly deri sumka, gara", 990, false),
            new Item(28, "Кожаная сумка с люверсами", "Lýuwersli deri sumka", 950, false),
            new Item(29, "Нейлоновая сумка-мессенджер", "Neýlon messenjer-sumka", 890, false),
            new Item(30, "Нейлоновая сумка-мешок", "Neýlon halta-sumka", 820, false),
            new Item(31, "Кожаная сумка-тоут, чёрная", "Deri tout-sumka, gara", 1250, false),
            new Item(32, "Кожаная сумка с ромбовидной стёжкой", "Rombly stežkaly deri sumka", 1350, true),
            new Item(38, "Кожаная сумка-кейс с 3D-цветами", "3D gülli deri keýs-sumka", 1190, false),
            new Item(39, "Кожаная сумка-хобо в складку", "Epinli deri hobo-sumka", 1090, false),
            new Item(55, "Кожаная сумка с ручкой, карамель", "Tutawaçly deri sumka, karamel", 1290, false));

    private static final List<Item> EVENING = List.of(
            new Item(10, "Вечерний клатч-кольчуга, золото", "Agşamky zynjyr klatç, altyn", 990, false),
            new Item(15, "Клатч со стразами, розовый", "Strazly klatç, gülgüne", 1090, false),
            new Item(41, "Вечерняя сумочка с кристаллами, голубая", "Kristally agşam sumkajygy, gök", 1490, true),
            new Item(43, "Вечерняя сумочка с кристаллами, карамель", "Kristally agşam sumkajygy, karamel", 1490, false),
            new Item(44, "Вечерняя сумочка с кристаллами, белая", "Kristally agşam sumkajygy, ak", 1490, false),
            new Item(45, "Сумочка-мешок в складку, сиреневая", "Epinli halta-sumkajyk, syrenç", 950, false),
            new Item(46, "Вечерняя сумочка с кристаллами, розовая", "Kristally agşam sumkajygy, gülgüne", 1490, true));

    private static final List<CatalogSection> CATALOG = List.of(
            new CatalogSection("Обувь", "Aýakgap", SHOES,
                    n -> "«" + n + "» из коллекции DELJE, Aşgabat. Женская обувь ручного отбора — "
                            + "качественные материалы и аккуратная посадка. Размеры уточняйте в Direct @delje_brand.",
                    n -> "«" + n + "» — DELJE kolleksiýasyndan, Aşgabat. El bilen saýlanan zenan aýakgaby — "
                            + "hilli materiallar we amatly gonuş. Ölçegleri @delje_brand Direct-da anyklaň."),
            new CatalogSection("Сумки", "Sumkalar", BAGS,
                    n -> "«" + n + "» из коллекции DELJE, Aşgabat. Стильная женская сумка на каждый день. "
                            + "Оформление заказа — в Direct @delje_brand.",
                    n -> "«" + n + "» — DELJE kolleksiýasyndan, Aşgabat. Her güne ýaraýan nusgawy zenan sumkasy. "
                            + "Sargyt @delje_brand Direct arkaly."),
            new CatalogSection("Вечерние сумки", "Agşam sumkalary", EVENING,
                    n -> "«" + n + "» из коллекции DELJE, Aşgabat. Вечерний аксессуар для особых случаев — "
                            + "финальный штрих к образу. Оформление заказа — в Direct @delje_brand.",
                    n -> "«" + n + "» — DELJE kolleksiýasyndan, Aşgabat. Aýratyn pursatlar üçin agşamky esbap — "
                            + "keşbiňe jemleýji öwüşgin. Sargyt @delje_brand Direct arkaly."));

    private static final List<BannerItem> BANNERS = List.of(
            new BannerItem("04_web_1200x628.png", "Новинки", "Täze gelenler", "/products?sort=new"),
            new BannerItem("06_web_sumkalar_1200x628.png", "Сумки", "Sumkalar", "/products?category=sumki"),
            new BannerItem("07_web_ayakgap_1200x628.png", "Обувь", "Aýakgap", "/products?category=obuv"),
            new BannerItem("08_web_sargyt_1200x628.png", "Под заказ", "Sargyt", "/contact"),
            new BannerItem("09_web_arzanladys_1200x628.png", "Скидки", "Arzanladyş", "/products"));

    private final AppProperties properties;
    private final ImageService imageService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BannerRepository bannerRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataImportService(AppProperties properties, ImageService imageService,
                             CategoryRepository categoryRepository, ProductRepository productRepository,
                             BannerRepository bannerRepository, JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.imageService = imageService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.bannerRepository = bannerRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void importCatalog() throws IOException {
        Path importDir = Paths.get(properties.getImportDir()).toAbsolutePath();
        Path productsDir = importDir.resolve("products");
        Path bannersDir = importDir.resolve("banners");
        List<String> productFiles;
        try (Stream<Path> files = Files.list(productsDir)) {
            productFiles = files.map(p -> p.getFileName().toString()).sorted().toList();
        }

        truncateCatalog(jdbcTemplate);

        Set<String> categorySlugs = new HashSet<>();
        Set<String> productSlugs = new HashSet<>();
        int productCount = 0;
        int topCount = 0;

        for (CatalogSection section : CATALOG) {
            Category category = new Category();
            category.setName(section.name());
            category.setNameTk(section.nameTk());
            category.setSlug(uniqueInSet(categorySlugs, SlugUtils.slugify(section.name())));
            categoryRepository.save(category);

            for (Item item : section.items()) {
                Path photo = productsDir.resolve(productFile(productFiles, item.photo(), productsDir));
                ImageService.StoredImage stored = imageService.saveProductImage(Files.readAllBytes(photo));

                Product product = new Product();
                product.setName(item.name());
                product.setNameTk(item.nameTk());
                product.setSlug(uniqueInSet(productSlugs, SlugUtils.slugify(item.name())));
                product.setDescription(section.description().apply(item.name()));
                product.setDescriptionTk(section.descriptionTk().apply(item.nameTk()));
                product.setPrice(BigDecimal.valueOf(item.price()));
                product.setTop(item.top());
                product.setCategory(category);

                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setThumb(stored.thumb());
                image.setFull(stored.full());
                image.setSort(0);
                product.getImages().add(image);
                productRepository.save(product);

                productCount++;
                if (item.top()) {
                    topCount++;
                }
            }
        }

        for (BannerItem item : BANNERS) {
            String image = imageService.saveBanner(Files.readAllBytes(bannersDir.resolve(item.file())));
            Banner banner = new Banner();
            banner.setImage(image);
            banner.setTitle(item.title());
            banner.setTitleTk(item.titleTk());
            banner.setLink(item.link());
            banner.setActive(true);
            bannerRepository.save(banner);
        }

        log.info("import finished: {} categories, {} products ({} top), {} images, {} banners",
                CATALOG.size(), productCount, topCount, productCount, BANNERS.size());
    }

    static void truncateCatalog(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("TRUNCATE TABLE product_images, products, categories, banners RESTART IDENTITY CASCADE");
    }

    static String uniqueInSet(Set<String> seen, String base) {
        String slug = base;
        int i = 2;
        while (seen.contains(slug)) {
            slug = base + "-" + i;
            i++;
        }
        seen.add(slug);
        return slug;
    }

    // photos are named tovar_NN.png or tovar_NN_msg.png
    private static String productFile(List<String> files, int number, Path dir) {
        String prefix = String.format("tovar_%02d", number);
        return files.stream()
                .filter(f -> f.startsWith(prefix) && f.endsWith(".png"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("photo " + prefix + "* not found in " + dir));
    }
}
