package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.value.Nutrients;

import java.util.List;

public record MonthCard(int month, String column, List<String> newInPeriod, List<AllowedItem> allowed, Needs needs,
                        MilkInfo milk, Nutrients prikormTarget, List<DayVariant> exampleDay, List<TextFact> optional,
                        List<TextFact> rules, List<TextFact> restrictions, List<String> notes, List<String> warnings) {
}
