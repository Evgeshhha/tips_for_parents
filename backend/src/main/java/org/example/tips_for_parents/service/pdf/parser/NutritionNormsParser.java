package org.example.tips_for_parents.service.pdf.parser;

import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.pdf.DailyNutritionNorm;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public final class NutritionNormsParser {

    private static final Pattern BAND = Pattern.compile("(\\d+)\\s*[–-]\\s*(\\d+)\\s*мес");

    public static List<DailyNutritionNorm> parse(List<List<String>> rows) {
        int h = headerRow(rows, "энерги");
        List<String> header = rows.get(h);
        int ie = col(header, "энерги"), ip = col(header, "белк"), iff = col(header, "жир"), ic = col(header, "углевод");

        List<DailyNutritionNorm> out = new ArrayList<>();
        for (int i = h + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Matcher m = BAND.matcher(NumberParsingUtils.clean(r.get(0)));
            if (!m.find()) continue;
            var e = NumberParsingUtils.parseRange(r.get(ie));
            var p = NumberParsingUtils.parseRange(r.get(ip));
            var f = NumberParsingUtils.parseRange(r.get(iff));
            var c = NumberParsingUtils.parseRange(r.get(ic));
            if (e == null || p == null || f == null || c == null) continue;
            out.add(new DailyNutritionNorm(NumberParsingUtils.clean(r.get(0)), Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    e.lo(), p.lo(), f.lo(), c.lo()));
        }
        if (out.isEmpty()) throw new IllegalStateException("В приложении 1 не найдено ни одной строки норм");
        return out;
    }

    static int headerRow(List<List<String>> rows, String key) {
        for (int i = 0; i < rows.size(); i++)
            for (String c : rows.get(i)) if (NumberParsingUtils.clean(c).toLowerCase().contains(key)) return i;
        throw new IllegalStateException("Не найдена строка заголовка с «" + key + "»");
    }

    static int col(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++)
            if (NumberParsingUtils.clean(header.get(i)).toLowerCase().contains(key)) return i;
        throw new IllegalStateException("Не найден столбец «" + key + "»");
    }
}
