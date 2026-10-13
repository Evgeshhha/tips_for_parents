package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.value.Nutrients;

public record Item(String name, String amount, String unit, Nutrients nutrients, long factId) {
}

