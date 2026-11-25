package org.example.tips_for_parents.service.card;

import org.example.tips_for_parents.model.card.Needs;
import org.example.tips_for_parents.model.card.Row;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.pdf.FoodNutrientProfile;
import org.example.tips_for_parents.model.value.Nutrients;

import java.util.List;

/** Всё, что нужно для сборки варианта дня и что одинаково для всех вариантов месяца. */
record DayContext(List<Row> allRows, List<FoodNutrientProfile> nutrients, Needs needs, Nutrients target,
                  Nutrients milkDay, List<FeedingFact> subjectRules) {
}
