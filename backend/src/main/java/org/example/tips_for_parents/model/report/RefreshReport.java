package org.example.tips_for_parents.model.report;

import java.util.List;

public record RefreshReport(List<SourceImportReport> sources, AutoReviewReport autoReview) {
}