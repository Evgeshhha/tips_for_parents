package org.example.tips_for_parents.model.pdf;

public record ExtractedFactDraft(int block, String type, String subject, String valueText, Integer valueMin, Integer valueMax,
                                 String sentence, String payload, String confidence) {
}
