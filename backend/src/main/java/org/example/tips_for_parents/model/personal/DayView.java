package org.example.tips_for_parents.model.personal;

import com.fasterxml.jackson.databind.JsonNode;

public record DayView(int seq, String newProductCode, JsonNode day) {
}
