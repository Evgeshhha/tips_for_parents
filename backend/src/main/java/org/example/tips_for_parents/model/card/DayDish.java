package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.value.Nutrients;

import java.util.List;

public record DayDish(String name, String amount, String unit, String tableRange, String vsTable, String amountPolicy,
                      Nutrients nutrients, List<Item> addons, List<Hint> hints, long factId) {
}
