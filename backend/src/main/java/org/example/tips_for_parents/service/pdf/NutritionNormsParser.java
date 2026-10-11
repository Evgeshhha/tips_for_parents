package org.example.tips_for_parents.service.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Приложение 1: нормы энергии и макронутриентов на 1 кг массы тела в сутки по возрастным группам. */
public final class NutritionNormsParser {

    public record Norm(String label, int monthFrom, int monthTo, double energy, double protein, double fat, double carbs) {}

    private static final Pattern BAND = Pattern.compile("(\\d+)\\s*[–-]\\s*(\\d+)\\s*мес");

    private NutritionNormsParser() {}

    public static List<Norm> parse(List<List<String>> rows) {
        int h = headerRow(rows, "энерги");
        List<String> header = rows.get(h);
        int ie = col(header, "энерги"), ip = col(header, "белк"), iff = col(header, "жир"), ic = col(header, "углевод");

        List<Norm> out = new ArrayList<>();
        for (int i = h + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Matcher m = BAND.matcher(NumberParser.clean(r.get(0)));
            if (!m.find()) continue;
            var e = NumberParser.parseRange(r.get(ie));
            var p = NumberParser.parseRange(r.get(ip));
            var f = NumberParser.parseRange(r.get(iff));
            var c = NumberParser.parseRange(r.get(ic));
            if (e == null || p == null || f == null || c == null) continue;   // строку с нечитаемыми числами не берём
            out.add(new Norm(NumberParser.clean(r.get(0)), Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    e.lo(), p.lo(), f.lo(), c.lo()));
        }
        if (out.isEmpty()) throw new IllegalStateException("В приложении 1 не найдено ни одной строки норм");
        return out;
    }

    static int headerRow(List<List<String>> rows, String key) {
        for (int i = 0; i < rows.size(); i++)
            for (String c : rows.get(i)) if (NumberParser.clean(c).toLowerCase().contains(key)) return i;
        throw new IllegalStateException("Не найдена строка заголовка с «" + key + "»");
    }

    static int col(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++)
            if (NumberParser.clean(header.get(i)).toLowerCase().contains(key)) return i;
        throw new IllegalStateException("Не найден столбец «" + key + "»");
    }
}
