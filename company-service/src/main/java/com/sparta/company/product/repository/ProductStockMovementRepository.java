package com.sparta.company.product.repository;

import com.sparta.company.product.entity.ProductStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductStockMovementRepository extends JpaRepository<ProductStockMovement, UUID> {

    boolean existsByProductIdAndReferenceIdAndMovementType(
            UUID productId, String referenceId, ProductStockMovement.MovementType movementType);
}
