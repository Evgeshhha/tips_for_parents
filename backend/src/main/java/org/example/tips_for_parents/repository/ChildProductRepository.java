package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.model.entity.ChildProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildProductRepository extends JpaRepository<ChildProduct, Long> {
    List<ChildProduct> findByChildId(UUID childId);

    Optional<ChildProduct> findByChildIdAndProductCode(UUID childId, String productCode);
}
