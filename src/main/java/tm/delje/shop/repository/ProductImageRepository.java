package tm.delje.shop.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tm.delje.shop.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProductIdInOrderBySortAscIdAsc(Collection<Integer> productIds);
}
