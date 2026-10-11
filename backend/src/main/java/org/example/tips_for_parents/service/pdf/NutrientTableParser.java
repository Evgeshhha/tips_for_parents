package org.example.tips_for_parents.service.pdf;

import org.example.tips_for_parents.model.records.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Таблица 7.4: пищевая ценность продуктов и блюд прикорма. Основание — «на 100 г» или указано в названии («Сливочное масло, 5 г»). */
public final class NutrientTableParser {

    public record Nutrient(String name, double basisAmount, String basisUnit,
                           Range protein, Range fat, Range carbs, Range energy) {}

    private static final Pattern BASIS =
            Pattern.compile("^(.*?),\\s*(\\d+/\\d+|\\d+(?:[.,]\\d+)?)\\s*(г|мл|шт\\.?)\\s*$");

    private NutrientTableParser() {}

    public static List<Nutrient> parse(List<List<String>> rows) {
        int h = NutritionNormsParser.headerRow(rows, "белк");
        List<String> header = rows.get(h);
        int ip = NutritionNormsParser.col(header, "белк"), iff = NutritionNormsParser.col(header, "жир"),
                ic = NutritionNormsParser.col(header, "углевод"), ie = NutritionNormsParser.col(header, "энергетическ");

        List<Nutrient> out = new ArrayList<>();
        for (int i = h + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String name = NumberParser.clean(r.get(0));
            if (name.isEmpty()) continue;
            Range p = NumberParser.parseRange(r.get(ip)), f = NumberParser.parseRange(r.get(iff)),
                    c = NumberParser.parseRange(r.get(ic)), e = NumberParser.parseRange(r.get(ie));
            if (p == null || f == null || c == null || e == null) continue;

            double basisAmount = 100;
            String basisUnit = "г";
            Matcher m = BASIS.matcher(name);
            if (m.matches()) {
                name = m.group(1).strip();
                basisAmount = amount(m.group(2));
                basisUnit = m.group(3).replace(".", "");
            }
            out.add(new Nutrient(name, basisAmount, basisUnit, p, f, c, e));
        }
        if (out.isEmpty()) throw new IllegalStateException("В таблице 7.4 не найдено ни одной строки");
        return out;
    }

    private static double amount(String s) {
        if (s.contains("/")) {
            String[] p = s.split("/");
            return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
        }
        return NumberParser.d(s);
    }
}
