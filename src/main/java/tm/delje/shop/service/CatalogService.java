package tm.delje.shop.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tm.delje.shop.entity.Banner;
import tm.delje.shop.entity.Category;
import tm.delje.shop.entity.Product;
import tm.delje.shop.entity.ProductImage;
import tm.delje.shop.repository.BannerRepository;
import tm.delje.shop.repository.CategoryRepository;
import tm.delje.shop.repository.ProductImageRepository;
import tm.delje.shop.repository.ProductRepository;

/**
 * Read-only queries of the public showcase.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final int CATALOG_PAGE_SIZE = 24;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;

    public CatalogService(ProductRepository productRepository, ProductImageRepository productImageRepository,
                          CategoryRepository categoryRepository, BannerRepository bannerRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
    }

    public List<Banner> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByIdAsc();
    }

    public List<Category> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public List<Product> getTopProducts() {
        return withMainImages(productRepository.findTop8ByTopTrueOrderByCreatedAtDescIdDesc());
    }

    public List<Product> getNewProducts() {
        return withMainImages(productRepository.findTop8ByOrderByCreatedAtDescIdDesc());
    }

    public Map<Integer, Long> countProductsByCategory() {
        Map<Integer, Long> result = new HashMap<>();
        for (Object[] row : productRepository.countByCategory()) {
            result.put((Integer) row[0], (Long) row[1]);
        }
        return result;
    }

    public Page<Product> search(Integer categoryId, String q, BigDecimal min, BigDecimal max, String sort, int page) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (q != null && !q.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase(Locale.ROOT) + "%"));
            }
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), max));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page - 1, CATALOG_PAGE_SIZE, sortFor(sort));
        Page<Product> result = productRepository.findAll(spec, pageRequest);
        withMainImages(result.getContent());
        return result;
    }

    public Optional<Product> findBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    public List<Product> getRelated(Product product) {
        if (product.getCategoryId() == null) {
            return List.of();
        }
        return withMainImages(productRepository
                .findTop4ByCategoryIdAndIdNotOrderByCreatedAtDescIdDesc(product.getCategoryId(), product.getId()));
    }

    /**
     * Loads the first image of every product with one query (cards show only one picture).
     */
    public List<Product> withMainImages(List<Product> products) {
        if (products.isEmpty()) {
            return products;
        }
        List<Integer> ids = products.stream().map(Product::getId).toList();
        Map<Integer, ProductImage> first = new HashMap<>();
        for (ProductImage image : productImageRepository.findByProductIdInOrderBySortAscIdAsc(ids)) {
            first.putIfAbsent(image.getProductId(), image);
        }
        for (Product product : products) {
            product.setMainImage(first.get(product.getId()));
        }
        return products;
    }

    private Sort sortFor(String sort) {
        return switch (sort) {
            case "old" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case "cheap" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "expensive" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            case "a-z" -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            case "z-a" -> Sort.by(Sort.Order.desc("name"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }
}
