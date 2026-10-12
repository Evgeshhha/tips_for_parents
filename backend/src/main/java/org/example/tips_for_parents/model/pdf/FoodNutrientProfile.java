package org.example.tips_for_parents.model.pdf;

import org.example.tips_for_parents.model.records.NumericRange;

public record FoodNutrientProfile(String name, double basisAmount, String basisUnit, NumericRange protein, NumericRange fat, NumericRange carbs,
                                  NumericRange energy) {
}

