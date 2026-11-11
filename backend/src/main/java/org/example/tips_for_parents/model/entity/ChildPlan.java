package org.example.tips_for_parents.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.tips_for_parents.model.enums.PlanStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Версия персонального плана. При каждом пересчёте создаётся новая, предыдущая -> SUPERSEDED. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "child_plan")
public class ChildPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id")
    private Child child;
    @Column(nullable = false)
    private int version;
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanStatus status = PlanStatus.ACTIVE;
    @Column(length = 200)
    private String reason;
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("planMonth ASC")
    private List<ChildPlanMonth> months = new ArrayList<>();
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("planMonth ASC, seq ASC")
    private List<ChildPlanDay> days = new ArrayList<>();

    public ChildPlan(Child child, int version, String reason) {
        this.child = child;
        this.version = version;
        this.reason = reason;
    }

    public void addMonth(ChildPlanMonth m) {
        m.setPlan(this);
        months.add(m);
    }

    public void addDay(ChildPlanDay d) {
        d.setPlan(this);
        days.add(d);
    }
}
