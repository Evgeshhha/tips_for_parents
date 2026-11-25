package org.example.tips_for_parents.model.enums;

/** Значения колонки feeding_fact.fact_type. В БД хранится строка, поэтому это константы, а не enum. */
public final class FactType {
    private FactType() {
    }

    public static final String TABLE_AGE = "TABLE_AGE";
    public static final String NORM = "NORM";
    public static final String NUTRIENT = "NUTRIENT";
    public static final String WEIGHT_GAIN = "WEIGHT_GAIN";
    public static final String MILK_COMPOSITION = "MILK_COMPOSITION";
    public static final String MILK_VOLUME = "MILK_VOLUME";
    public static final String RULE = "RULE";
    public static final String RESTRICTION = "RESTRICTION";
    public static final String SCHEDULE = "SCHEDULE";
    public static final String OPTIONAL = "OPTIONAL";
    public static final String SLEEP_AGE = "SLEEP_AGE";
    public static final String SLEEP_TEXT = "SLEEP_TEXT";
}
