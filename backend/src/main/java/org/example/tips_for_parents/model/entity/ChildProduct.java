package org.example.tips_for_parents.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.enums.ProductStatus;

import java.time.Instant;
import java.time.LocalDate;

/** Статус одного продукта у одного ребёнка. productCode — ключ из MonthCardService.productKey(название строки таблицы 5.1). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "child_product", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "product_code"}))
public class ChildProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(name = "product_code", nullable = false, length = 200)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductStatus status;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ChildProduct(Child child, String productCode, ProductStatus status, LocalDate startedOn) {
        this.child = child;
        this.productCode = productCode;
        this.status = status;
        this.startedOn = startedOn;
    }

    public void update(ProductStatus status, LocalDate startedOn) {
        this.status = status;
        if (startedOn != null) this.startedOn = startedOn;
        this.updatedAt = Instant.now();
    }
}
