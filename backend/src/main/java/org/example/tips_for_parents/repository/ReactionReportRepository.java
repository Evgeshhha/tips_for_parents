package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.model.entity.ReactionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReactionReportRepository extends JpaRepository<ReactionReport, UUID> {
    Optional<ReactionReport> findByIdAndChildId(UUID id, UUID childId);

    List<ReactionReport> findByChildIdOrderByCreatedAtDesc(UUID childId);
}
