package org.example.tips_for_parents.model.pdf;

public record FeedingRecommendationRule(String type, String text, String subject, Integer monthMin, Integer monthMax, String payload) {
}
