package org.example.tips_for_parents.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.tips_for_parents.model.enums.FeedingType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "child")
public class Child {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(length = 100)
    private String nickname;
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    @Column(name = "birth_weight_kg")
    private Double birthWeightKg;
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "feeding_type", nullable = false, length = 16)
    private FeedingType feedingType;
    @Setter
    @Column(name = "milk_ml")
    private Integer milkMl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Child(String nickname, LocalDate birthDate, Double birthWeightKg, FeedingType feedingType, Integer milkMl) {
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.birthWeightKg = birthWeightKg;
        this.feedingType = feedingType;
        this.milkMl = milkMl;
    }
}
