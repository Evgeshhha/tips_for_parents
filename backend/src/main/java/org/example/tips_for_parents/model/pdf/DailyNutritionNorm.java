package org.example.tips_for_parents.model.pdf;

public record DailyNutritionNorm(String label, int monthFrom, int monthTo, double energy, double protein, double fat, double carbs) {
}
