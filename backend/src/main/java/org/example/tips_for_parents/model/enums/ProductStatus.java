package org.example.tips_for_parents.model.enums;

/** Статус продукта у ребёнка. Нет записи = действует обычная логика таблицы 5.1. NOT_STARTED/INTRODUCING = «новый» продукт, INTRODUCED = привычный, PAUSED/EXCLUDED = убран из дня. */
public enum ProductStatus { NOT_STARTED, INTRODUCING, INTRODUCED, PAUSED, EXCLUDED }
