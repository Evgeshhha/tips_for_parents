package org.example.tips_for_parents.model.personal;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record MonthView(int month, JsonNode summary, List<DayView> days) {
}
