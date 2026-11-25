package org.example.tips_for_parents.service.card;

import org.example.tips_for_parents.model.card.Row;
import org.example.tips_for_parents.model.value.NumericRange;
import org.example.tips_for_parents.model.value.Nutrients;

/** Блюдо (или добавка к нему) в примерном дне во время расчёта граммовок. */
final class DishEntry {
    final Row row;
    final DishEntry parent;
    final NumericRange table;
    final boolean fraction;
    final Nutrients perUnit;
    double amount;
    String policy;

    DishEntry(Row row, DishEntry parent, NumericRange table, boolean fraction, Nutrients perUnit) {
        this.row = row;
        this.parent = parent;
        this.table = table;
        this.fraction = fraction;
        this.perUnit = perUnit;
    }

    double typical() {
        return table.lo() == table.hi() ? table.lo() : (table.lo() + table.hi()) / 2;
    }

    String unit() {
        return fraction ? "шт." : (row.unit() == null || row.unit().isBlank() ? "г, мл" : row.unit());
    }
}
