package org.example.tips_for_parents.model.dto;

import org.example.tips_for_parents.model.entity.ChildProduct;
import org.example.tips_for_parents.model.enums.ProductStatus;

import java.time.LocalDate;

public record ProductStatusDto(String productCode, ProductStatus status, LocalDate startedOn) {
    public static ProductStatusDto of(ChildProduct p) {
        return new ProductStatusDto(p.getProductCode(), p.getStatus(), p.getStartedOn());
    }
}
