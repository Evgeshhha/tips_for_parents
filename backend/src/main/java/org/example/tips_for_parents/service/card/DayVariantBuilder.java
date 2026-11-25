package org.example.tips_for_parents.service.card;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.config.CardsProperties;
import org.example.tips_for_parents.model.card.*;
import org.example.tips_for_parents.model.pdf.FoodNutrientProfile;
import org.example.tips_for_parents.model.value.NumericRange;
import org.example.tips_for_parents.model.value.Nutrients;
import org.example.tips_for_parents.model.value.VarSolve;
import org.example.tips_for_parents.service.pdf.util.TextNormalizationUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Собирает один вариант примерного дня: набор блюд, граммовки (по нормам и БЖУ), итоги и подсказки. */
@Component
@RequiredArgsConstructor
public class DayVariantBuilder {

    private static final String POLICY_NEW = "новый продукт вводят постепенно — нижняя граница диапазона таблицы 5.1";
    private static final String POLICY_CALC = "рассчитано по БЖУ: остаток нормы после молока/смеси, не выше таблицы 5.1";
    private static final String POLICY_NO_DATA = "по таблице 5.1: пищевой ценности блюда нет в таблице 7.4";
    private static final String POLICY_TABLE = "по таблице 5.1 (нормы или вес не определены — расчёт невозможен)";
    private static final double LAMBDA = 0.05;
    private static final int SWEEPS = 500;

    private final CardsProperties props;

    DayVariant build(DayContext ctx, String title, String newProduct, String note, List<Row> dishRows, Row newRow,
                     boolean startOfFeeding) {
        List<DishEntry> entries = new ArrayList<>();
        List<String> noData = new ArrayList<>();
        for (Row r : dishRows) {
            DishEntry main = entry(r, null, ctx.nutrients());
            if (main == null) { noData.add(r.label() + " (количество не разобрано)"); continue; }
            entries.add(main);
            for (Row a : ctx.allRows()) {
                if (a.addonStem() == null || !TextNormalizationUtils.matches(r.label(), a.addonStem())) continue;
                DishEntry add = entry(a, main, ctx.nutrients());
                if (add != null) entries.add(add);
            }
        }

        Nutrients target = ctx.target();
        boolean calc = target != null;
        double[] fixed = new double[3];
        List<VarSolve> varSolves = new ArrayList<>();
        List<DishEntry> varEntries = new ArrayList<>();
        for (DishEntry e : entries) {
            if (e.row == newRow || startOfFeeding) { e.amount = e.table.lo(); e.policy = POLICY_NEW; }
            else if (e.perUnit == null) { e.amount = e.typical(); e.policy = POLICY_NO_DATA; }
            else if (!calc) { e.amount = e.typical(); e.policy = POLICY_TABLE; }
            else {
                varSolves.add(new VarSolve(PortionMath.mid(e.perUnit), props.minPortionShare() * e.table.hi(), e.table.hi(), e.typical()));
                varEntries.add(e);
                continue;
            }
            if (e.perUnit != null) {
                double[] d = PortionMath.mid(e.perUnit);
                for (int k = 0; k < 3; k++) fixed[k] += d[k] * e.amount;
            }
        }
        if (!varSolves.isEmpty()) {
            double[] t = {Math.max(target.protein().lo() - fixed[0], 0), Math.max(target.fat().lo() - fixed[1], 0),
                    Math.max(target.carbs().lo() - fixed[2], 0)};
            double[] x = DaySolver.solve(varSolves, t, LAMBDA, SWEEPS);
            for (int i = 0; i < x.length; i++) {
                DishEntry e = varEntries.get(i);
                VarSolve v = varSolves.get(i);
                double r = PortionMath.roundAmount(x[i], e.fraction);
                e.amount = Math.min(v.hi(), Math.max(Math.min(v.lo(), v.hi()), r));
                e.policy = POLICY_CALC;
            }
        }

        Nutrients totals = Nutrients.ZERO;
        List<DayDish> dishes = new ArrayList<>();
        for (DishEntry e : entries) {
            if (e.perUnit != null) totals = totals.plus(e.perUnit.times(e.amount));
            else noData.add(e.row.label());
        }
        for (DishEntry e : entries) {
            if (e.parent != null) continue;
            List<Item> addons = entries.stream().filter(a -> a.parent == e)
                    .map(a -> new Item(a.row.label(), PortionMath.display(a.amount, a.fraction), a.unit(),
                            a.perUnit == null ? null : a.perUnit.times(a.amount).rounded(), a.row.fact().getId())).toList();
            List<Hint> hints = ctx.subjectRules().stream().filter(s -> TextNormalizationUtils.matches(e.row.label(), s.getSubject()))
                    .map(s -> new Hint(s.getSentence(), s.getId())).toList();
            dishes.add(new DayDish(e.row.label(), PortionMath.display(e.amount, e.fraction), e.unit(), e.row.cell().amount(),
                    PortionMath.vsTable(e.amount, e.table), e.policy, e.perUnit == null ? null : e.perUnit.times(e.amount).rounded(),
                    addons, hints, e.row.fact().getId()));
        }

        Nutrients pctTarget = target == null ? null : totals.percentOf(target).rounded();
        Nutrients dayTotals = ctx.milkDay() == null ? null : totals.plus(ctx.milkDay()).rounded();
        Nutrients pctNeed = ctx.needs() == null || ctx.milkDay() == null ? null
                : totals.plus(ctx.milkDay()).percentOf(ctx.needs().perDay()).rounded();
        return new DayVariant(title, newProduct, note, dishes, totals.rounded(), pctTarget, dayTotals, pctNeed,
                noData.isEmpty() ? List.of() : List.of("Нет данных о пищевой ценности в программе (таблица 7.4): "
                        + String.join(", ", noData)));
    }

    private DishEntry entry(Row r, DishEntry parent, List<FoodNutrientProfile> nuts) {
        NumericRange range = PortionMath.parseAmount(r.label(), r.cell().amount());
        if (range == null) return null;
        return new DishEntry(r, parent, range, PortionMath.isFraction(r.cell().amount()), PortionMath.perUnit(r.label(), nuts));
    }
}
