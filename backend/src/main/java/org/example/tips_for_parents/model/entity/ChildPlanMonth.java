package org.example.tips_for_parents.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Итоги месяца для конкретного ребёнка: потребность (с реальной массой), молоко, цель для прикорма, предупреждения. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "child_plan_month")
public class ChildPlanMonth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private ChildPlan plan;

    @Column(name = "plan_month", nullable = false)
    private int planMonth;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode summary;

    public ChildPlanMonth(int planMonth, JsonNode summary) {
        this.planMonth = planMonth;
        this.summary = summary;
    }
}
