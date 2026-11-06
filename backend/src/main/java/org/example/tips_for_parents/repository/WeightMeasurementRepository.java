package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.model.entity.WeightMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WeightMeasurementRepository extends JpaRepository<WeightMeasurement, Long> {
    Optional<WeightMeasurement> findFirstByChildIdOrderByMeasuredOnDescIdDesc(UUID childId);
}
