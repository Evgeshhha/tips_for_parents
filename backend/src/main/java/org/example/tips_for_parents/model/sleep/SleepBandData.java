package org.example.tips_for_parents.model.sleep;

/**
 * Числа одной строки таблицы НМИЦ «Режим дня для ребенка до 1 года».
 * Всё, что в источнике записано диапазоном («3-4 раза по 1,5-2 часа»), хранится как min/max;
 * одиночное значение даёт min == max. Если ячейка не числовая («Во время кормления»), числовые поля null,
 * а исходный текст всегда лежит в *Text.
 */
public record SleepBandData(Integer napsCountMin, Integer napsCountMax,
                            Double napHoursMin, Double napHoursMax,
                            Double nightHoursMin, Double nightHoursMax,
                            Integer wakeCountMin, Integer wakeCountMax,
                            Double wakeHoursMin, Double wakeHoursMax,
                            String napsText, String nightText, String wakeText) {
}
