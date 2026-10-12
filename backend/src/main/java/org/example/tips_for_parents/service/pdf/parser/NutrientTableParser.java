package org.example.tips_for_parents.service.pdf.parser;

import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.pdf.FoodNutrientProfile;
import org.example.tips_for_parents.model.value.NumericRange;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public final class NutrientTableParser {

    private static final Pattern BASIS =
            Pattern.compile("^(.*?),\\s*(\\d+/\\d+|\\d+(?:[.,]\\d+)?)\\s*(г|мл|шт\\.?)\\s*$");

    public static List<FoodNutrientProfile> parse(List<List<String>> rows) {
        int h = NutritionNormsParser.headerRow(rows, "белк");
        List<String> header = rows.get(h);
        int ip = NutritionNormsParser.col(header, "белк"), iff = NutritionNormsParser.col(header, "жир"),
                ic = NutritionNormsParser.col(header, "углевод"), ie = NutritionNormsParser.col(header, "энергетическ");

        List<FoodNutrientProfile> out = new ArrayList<>();
        for (int i = h + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String name = NumberParsingUtils.clean(r.get(0));
            if (name.isEmpty()) continue;
            NumericRange p = NumberParsingUtils.parseRange(r.get(ip)), f = NumberParsingUtils.parseRange(r.get(iff)),
                    c = NumberParsingUtils.parseRange(r.get(ic)), e = NumberParsingUtils.parseRange(r.get(ie));
            if (p == null || f == null || c == null || e == null) continue;

            double basisAmount = 100;
            String basisUnit = "г";
            Matcher m = BASIS.matcher(name);
            if (m.matches()) {
                name = m.group(1).strip();
                basisAmount = amount(m.group(2));
                basisUnit = m.group(3).replace(".", "");
            }
            out.add(new FoodNutrientProfile(name, basisAmount, basisUnit, p, f, c, e));
        }
        if (out.isEmpty()) throw new IllegalStateException("В таблице 7.4 не найдено ни одной строки");
        return out;
    }

    private static double amount(String s) {
        if (s.contains("/")) {
            String[] p = s.split("/");
            return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
        }
        return NumberParsingUtils.d(s);
    }
}
