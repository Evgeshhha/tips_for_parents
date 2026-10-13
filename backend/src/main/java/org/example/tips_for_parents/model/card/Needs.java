package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.value.Nutrients;

public record Needs(String ageBand, double weightKg, String weightSource, Nutrients perKg, Nutrients perDay) {
}

