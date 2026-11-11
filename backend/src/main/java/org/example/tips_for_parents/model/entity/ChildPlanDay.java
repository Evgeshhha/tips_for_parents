package org.example.tips_for_parents.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "child_plan_day")
public class ChildPlanDay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private ChildPlan plan;
    @Column(name = "plan_month", nullable = false)
    private int planMonth;
    @Column(nullable = false)
    private int seq;
    @Column(name = "new_product_code", length = 200)
    private String newProductCode;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    public ChildPlanDay(int planMonth, int seq, String newProductCode, JsonNode payload) {
        this.planMonth = planMonth;
        this.seq = seq;
        this.newProductCode = newProductCode;
        this.payload = payload;
    }
}
