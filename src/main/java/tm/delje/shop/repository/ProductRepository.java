package tm.delje.shop.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import tm.delje.shop.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    List<Product> findTop8ByTopTrueOrderByCreatedAtDescIdDesc();

    List<Product> findTop8ByOrderByCreatedAtDescIdDesc();

    List<Product> findTop4ByCategoryIdAndIdNotOrderByCreatedAtDescIdDesc(Integer categoryId, Integer id);

    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"images"})
    Optional<Product> findWithImagesById(Integer id);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Pageable pageable);

    @Query("select p.categoryId, count(p) from Product p where p.categoryId is not null group by p.categoryId")
    List<Object[]> countByCategory();

    @Query("select p.slug from Product p order by p.createdAt desc, p.id desc")
    List<String> findAllSlugs();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Integer id);
}
