package org.example.tips_for_parents.model.report;

import java.util.List;

public record SourceImportReport(String source, String status, Long documentId, int facts, List<String> notes) {
}