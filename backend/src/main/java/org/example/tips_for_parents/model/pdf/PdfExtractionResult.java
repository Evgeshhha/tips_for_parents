package org.example.tips_for_parents.model.pdf;

import java.util.List;

public record PdfExtractionResult(List<ExtractedBlockDraft> blocks, List<ExtractedFactDraft> facts, List<String> notes) {
}
