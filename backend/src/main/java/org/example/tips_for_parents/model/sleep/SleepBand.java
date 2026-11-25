package org.example.tips_for_parents.model.sleep;

/** Возрастной диапазон из утверждённого факта: label — подпись строки из источника, sentence — строка таблицы текстом. */
public record SleepBand(String label, int monthFrom, int monthTo, SleepBandData data, String sentence, long factId) {
}
