package tm.delje.shop.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tm.delje.shop.dto.CategoryForm;
import tm.delje.shop.entity.Category;
import tm.delje.shop.repository.CategoryRepository;
import tm.delje.shop.util.SlugUtils;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Optional<Category> findById(Integer id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public Category create(CategoryForm form) {
        Category category = new Category();
        category.setName(form.getName());
        category.setNameTk(emptyToNull(form.getNameTk()));
        category.setSlug(SlugUtils.uniqueSlug(form.getName(), categoryRepository::existsBySlug));
        return categoryRepository.save(category);
    }

    @Transactional
    public void update(Integer id, CategoryForm form) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("category " + id));
        category.setName(form.getName());
        category.setNameTk(emptyToNull(form.getNameTk()));
        category.setSlug(SlugUtils.uniqueSlug(form.getName(),
                slug -> categoryRepository.existsBySlugAndIdNot(slug, id)));
    }

    /**
     * Products of the category stay, their category_id becomes NULL (foreign key rule).
     */
    @Transactional
    public void delete(Integer id) {
        categoryRepository.findById(id).ifPresent(categoryRepository::delete);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
