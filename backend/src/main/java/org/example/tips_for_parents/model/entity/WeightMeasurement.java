package org.example.tips_for_parents.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weight_measurement")
public class WeightMeasurement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    public WeightMeasurement(Child child, LocalDate measuredOn, double weightKg) {
        this.child = child;
        this.measuredOn = measuredOn;
        this.weightKg = weightKg;
    }
}
