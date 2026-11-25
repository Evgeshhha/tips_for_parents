package org.example.tips_for_parents.service.card;

import org.example.tips_for_parents.model.entity.FeedingFact;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Утверждённые факты, сгруппированные по fact_type (см. {@link org.example.tips_for_parents.model.enums.FactType}). */
public record ApprovedFacts(Map<String, List<FeedingFact>> byType) {

    public static ApprovedFacts from(List<FeedingFact> approved) {
        return new ApprovedFacts(approved.stream().collect(Collectors.groupingBy(FeedingFact::getFactType)));
    }

    public List<FeedingFact> of(String factType) {
        return byType.getOrDefault(factType, List.of());
    }
}
