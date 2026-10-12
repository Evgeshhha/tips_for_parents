package org.example.tips_for_parents.model.pdf;

import org.example.tips_for_parents.model.records.NumericRange;

public record MonthlyWeightGain(String label, int monthFrom, int monthTo, NumericRange gramsPerDay) {
}
