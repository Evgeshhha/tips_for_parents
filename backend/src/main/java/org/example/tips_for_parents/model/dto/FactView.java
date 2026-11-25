package org.example.tips_for_parents.model.dto;

import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.ReviewStatus;

public record FactView(Long id, String type, String subject, String valueText, Integer valueMin, Integer valueMax,
                       String sentence, ReviewStatus status, String confidence, String reviewedBy, String reviewNote) {
    public static FactView of(FeedingFact f) {
        return new FactView(f.getId(), f.getFactType(), f.getSubject(), f.getValueText(), f.getValueMin(), f.getValueMax(), f.getSentence(),
                f.getReviewStatus(), f.getConfidence(), f.getReviewedBy(), f.getReviewNote());
    }
}
