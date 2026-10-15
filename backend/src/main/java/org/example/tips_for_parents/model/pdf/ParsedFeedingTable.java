package org.example.tips_for_parents.model.pdf;

import java.util.List;

public record ParsedFeedingTable(List<FeedingTableRow> feedingTableRows, String unit, List<String> skipped) {
}

