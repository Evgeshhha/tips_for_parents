package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.value.Nutrients;

import java.util.List;

public record DayVariant(String title, String newProduct, String note, List<DayDish> dishes, Nutrients totals,
                         Nutrients percentOfPrikormTarget, Nutrients dayTotalsWithMilk, Nutrients percentOfDailyNeed,
                         List<String> noNutrientData) {
}