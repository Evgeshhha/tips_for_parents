package org.example.tips_for_parents.model.sleep;

/** Разобранная строка таблицы: возрастной диапазон в месяцах (границы как в источнике) и числа. */
public record SleepRow(String label, int monthFrom, int monthTo, SleepBandData data) {
}
