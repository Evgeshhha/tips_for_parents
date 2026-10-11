package org.example.tips_for_parents.service.pdf;

import org.example.tips_for_parents.model.records.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Таблица 6.1: средняя прибавка массы тела (г/день) по возрастным группам. */
public final class WeightGainParser {

    public record Gain(String label, int monthFrom, int monthTo, Range gramsPerDay) {}

    private static final Pattern BAND = Pattern.compile("^(\\d+)\\s*[–-]\\s*(\\d+)$");

    private WeightGainParser() {}

    public static List<Gain> parse(List<List<String>> rows) {
        int h = NutritionNormsParser.headerRow(rows, "прибавка массы");
        int ig = NutritionNormsParser.col(rows.get(h), "прибавка массы");

        List<Gain> out = new ArrayList<>();
        for (int i = h + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Matcher m = BAND.matcher(NumberParser.clean(r.get(0)));
            if (!m.matches()) continue;
            Range g = NumberParser.parseRange(r.get(ig));
            if (g == null) continue;
            out.add(new Gain(NumberParser.clean(r.get(0)) + " мес", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), g));
        }
        if (out.isEmpty()) throw new IllegalStateException("В таблице 6.1 не найдено ни одной строки");
        return out;
    }
}
