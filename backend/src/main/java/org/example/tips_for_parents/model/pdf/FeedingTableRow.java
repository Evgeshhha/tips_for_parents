package org.example.tips_for_parents.model.pdf;

import java.util.List;

public record FeedingTableRow(String label, int minAge, List<FeedingAgeCell> feedingAgeCells, List<String> footnotes) {
}
