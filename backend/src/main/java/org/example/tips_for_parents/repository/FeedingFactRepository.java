package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface FeedingFactRepository extends JpaRepository<FeedingFact, Long> {
    List<FeedingFact> findByReviewStatusOrderById(ReviewStatus status);
    @Query("select f from FeedingFact f where f.block.document.id in "
            + "(select d.id from SourceDocument d where d.url = :url and d.id <> :docId) "
            + "and f.reviewStatus <> :superseded")
    List<FeedingFact> findActiveOfOtherVersions(@Param("url") String url, @Param("docId") Long docId, @Param("superseded") ReviewStatus superseded);
    @Query("select f.factType, f.reviewStatus, count(f) from FeedingFact f group by f.factType, f.reviewStatus")
    List<Object[]> countByTypeAndStatus();
}
