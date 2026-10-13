package org.example.tips_for_parents.model.payload;

import org.example.tips_for_parents.model.pdf.FeedingAgeCell;

import java.util.List;

public record TablePayload(String unit, List<FeedingAgeCell> cells, List<String> footnotes) {}

