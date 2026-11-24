package tm.delje.shop.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tm.delje.shop.dto.ProductForm;
import tm.delje.shop.entity.Category;
import tm.delje.shop.entity.Product;
import tm.delje.shop.entity.ProductImage;
import tm.delje.shop.repository.CategoryRepository;
import tm.delje.shop.repository.ProductRepository;
import tm.delje.shop.service.ImageService.StoredImage;
import tm.delje.shop.util.SlugUtils;

/**
 * Product management for the admin panel.
 * Pictures are converted before the database transaction, and files are removed again
 * if saving fails, so no orphan files are left behind.
 */
@Service
public class ProductService {

    public static final int ADMIN_PAGE_SIZE = 20;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogService catalogService;
    private final ImageService imageService;
    private final TransactionTemplate transactionTemplate;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          CatalogService catalogService, ImageService imageService,
                          PlatformTransactionManager transactionManager) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.catalogService = catalogService;
        this.imageService = imageService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public Page<Product> list(String q, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, ADMIN_PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Product> result;
        if (q == null || q.isEmpty()) {
            result = productRepository.findAll(pageRequest);
        } else {
            result = productRepository.findByNameContainingIgnoreCase(q, pageRequest);
        }
        catalogService.withMainImages(result.getContent());
        return result;
    }

    public Optional<Product> findWithImages(Integer id) {
        return productRepository.findWithImagesById(id);
    }

    public Product create(ProductForm form, List<MultipartFile> files) throws IOException {
        List<StoredImage> stored = storeImages(files);
        try {
            return transactionTemplate.execute(status -> {
                Product product = new Product();
                applyForm(product, form);
                product.setSlug(SlugUtils.uniqueSlug(form.getName(), productRepository::existsBySlug));
                for (int i = 0; i < stored.size(); i++) {
                    product.getImages().add(newImage(product, stored.get(i), i));
                }
                return productRepository.saveAndFlush(product);
            });
        } catch (RuntimeException e) {
            removeFiles(stored);
            throw e;
        }
    }

    public void update(Integer id, ProductForm form, List<MultipartFile> files) throws IOException {
        List<StoredImage> stored = storeImages(files);
        List<String> filesToRemove = new ArrayList<>();
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Product product = productRepository.findWithImagesById(id)
                        .orElseThrow(() -> new NotFoundException("product " + id));
                applyForm(product, form);
                // slug follows the base (Russian) name
                product.setSlug(SlugUtils.uniqueSlug(form.getName(),
                        slug -> productRepository.existsBySlugAndIdNot(slug, product.getId())));

                List<Integer> deleteIds = form.getDeleteImageIdValues();
                List<ProductImage> removed = product.getImages().stream()
                        .filter(img -> deleteIds.contains(img.getId()))
                        .toList();
                for (ProductImage image : removed) {
                    product.getImages().remove(image);
                    filesToRemove.add(image.getThumb());
                    filesToRemove.add(image.getFull());
                }

                // new pictures go after the current last one
                int sort = product.getImages().stream().mapToInt(ProductImage::getSort).max().orElse(-1) + 1;
                for (StoredImage image : stored) {
                    product.getImages().add(newImage(product, image, sort));
                    sort++;
                }
                productRepository.saveAndFlush(product);
            });
        } catch (RuntimeException e) {
            removeFiles(stored);
            throw e;
        }
        filesToRemove.forEach(imageService::deleteUpload);
    }

    public void delete(Integer id) {
        List<String> paths = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> productRepository.findWithImagesById(id).ifPresent(product -> {
            for (ProductImage image : product.getImages()) {
                paths.add(image.getThumb());
                paths.add(image.getFull());
            }
            productRepository.delete(product);
        }));
        paths.forEach(imageService::deleteUpload);
    }

    private void applyForm(Product product, ProductForm form) {
        product.setName(form.getName());
        product.setNameTk(emptyToNull(form.getNameTk()));
        product.setDescription(emptyToNull(form.getDescription()));
        product.setDescriptionTk(emptyToNull(form.getDescriptionTk()));
        product.setPrice(form.getPriceValue());
        product.setTop(form.getIsTop());

        Category category = null;
        Integer categoryId = form.getCategoryIdValue();
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }
        product.setCategory(category);
    }

    private List<StoredImage> storeImages(List<MultipartFile> files) throws IOException {
        List<StoredImage> stored = new ArrayList<>();
        if (files == null) {
            return stored;
        }
        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                stored.add(imageService.saveProductImage(file.getBytes()));
            }
        } catch (IOException | RuntimeException e) {
            removeFiles(stored);
            throw e;
        }
        return stored;
    }

    private void removeFiles(List<StoredImage> stored) {
        for (StoredImage image : stored) {
            imageService.deleteUpload(image.thumb());
            imageService.deleteUpload(image.full());
        }
    }

    private static ProductImage newImage(Product product, StoredImage stored, int sort) {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setThumb(stored.thumb());
        image.setFull(stored.full());
        image.setSort(sort);
        return image;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
