package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.model.entity.ChildPlan;
import org.example.tips_for_parents.model.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildPlanRepository extends JpaRepository<ChildPlan, UUID> {
    List<ChildPlan> findByChildIdAndStatus(UUID childId, PlanStatus status);

    Optional<ChildPlan> findFirstByChildIdAndStatus(UUID childId, PlanStatus status);

    Optional<ChildPlan> findFirstByChildIdOrderByVersionDesc(UUID childId);
}
