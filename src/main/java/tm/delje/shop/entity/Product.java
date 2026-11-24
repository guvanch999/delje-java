package tm.delje.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_tk")
    private String nameTk;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_tk", columnDefinition = "text")
    private String descriptionTk;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "is_top", nullable = false)
    private boolean top;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // read-only copy of the FK, handy when the category itself is not loaded
    @Column(name = "category_id", insertable = false, updatable = false)
    private Integer categoryId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sort ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    // first image for product cards, filled by the service for list pages
    @Transient
    private ProductImage mainImage;

    public String getLocalizedName(String lang) {
        return Localized.pick(name, nameTk, lang);
    }

    public String getLocalizedDescription(String lang) {
        return Localized.pick(description, descriptionTk, lang);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameTk() {
        return nameTk;
    }

    public void setNameTk(String nameTk) {
        this.nameTk = nameTk;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionTk() {
        return descriptionTk;
    }

    public void setDescriptionTk(String descriptionTk) {
        this.descriptionTk = descriptionTk;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        this.categoryId = category != null ? category.getId() : null;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public ProductImage getMainImage() {
        return mainImage;
    }

    public void setMainImage(ProductImage mainImage) {
        this.mainImage = mainImage;
    }
}
