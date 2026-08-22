package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySku(String sku);
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByBrandId(Long brandId);

    /** Used by CategoryServiceImpl.deleteCategory to block deletion cleanly (409) instead of a raw FK error. */
    boolean existsByCategoryId(Long categoryId);

    /** Used by BrandServiceImpl.deleteBrand — same purpose as existsByCategoryId above. */
    boolean existsByBrandId(Long brandId);

    /**
     * Atomic, single-statement stock decrement — the WHERE clause's stock >= qty
     * check happens in the same UPDATE as the write, so concurrent checkouts on
     * the last unit can't both succeed (no read-then-write race). Returns the
     * number of rows updated: 0 means insufficient stock (or the product no
     * longer exists), 1 means success. Callers must check the return value.
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") Integer qty);

    /** Used to restore stock on order cancellation/refund. */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") Integer qty);
}
