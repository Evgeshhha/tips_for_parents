package org.example.tips_for_parents.model.pdf;

import java.util.List;

public record ExtractedPdfTable(int pageNumber, List<List<String>> rows, String noteText) {
}
