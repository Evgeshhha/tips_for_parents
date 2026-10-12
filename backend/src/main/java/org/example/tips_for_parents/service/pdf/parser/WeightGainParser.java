package org.example.tips_for_parents.service.pdf.parser;

import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.pdf.MonthlyWeightGain;
import org.example.tips_for_parents.model.records.NumericRange;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public final class WeightGainParser {

    private static final Pattern BAND = Pattern.compile("^(\\d+)\\s*[–-]\\s*(\\d+)$");

    public static List<MonthlyWeightGain> parse(List<List<String>> rows) {
        int h = NutritionNormsParser.headerRow(rows, "прибавка массы");
        int ig = NutritionNormsParser.col(rows.get(h), "прибавка массы");

        List<MonthlyWeightGain> out = new ArrayList<>();
        for (int i = h + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Matcher m = BAND.matcher(NumberParsingUtils.clean(r.get(0)));
            if (!m.matches()) continue;
            NumericRange g = NumberParsingUtils.parseRange(r.get(ig));
            if (g == null) continue;
            out.add(new MonthlyWeightGain(NumberParsingUtils.clean(r.get(0)) + " мес", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), g));
        }
        if (out.isEmpty()) throw new IllegalStateException("В таблице 6.1 не найдено ни одной строки");
        return out;
    }
}
