package org.example.tips_for_parents.service.sleep;

import org.example.tips_for_parents.model.sleep.SleepBandData;
import org.example.tips_for_parents.model.sleep.SleepRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор таблицы «Возраст | Режим дневного сна | Ночной сон | Режим бодрствования» (НМИЦ здоровья детей).
 * Ничего не домысливает: строка, которую не удалось разобрать, попадает в skipped.
 */
public final class SleepTableParser {
    private SleepTableParser() {
    }

    public record Parsed(List<SleepRow> rows, List<String> skipped) {
    }

    private static final String NUM = "(\\d+(?:[.,]\\d+)?)";
    private static final String RANGE = NUM + "(?:\\s*[-\\u2013\\u2014]\\s*" + NUM + ")?";

    /** «6 раз по 2,5 часа», «3-4 раза по 1,5-2 часа» → группы 1-2 (сколько раз), 3-4 (часов за раз). */
    private static final Pattern TIMES_BY = Pattern.compile(
            "(?iu)^\\s*" + RANGE + "\\s*раз\\p{L}*\\s*по\\s*" + RANGE + "\\s*час\\p{L}*\\s*$");
    /** «6 часов», «6,5 часов», «9-10 часов». */
    private static final Pattern HOURS = Pattern.compile("(?iu)^\\s*" + RANGE + "\\s*час\\p{L}*\\s*$");
    private static final Pattern AGE_FROM_BIRTH = Pattern.compile("(?iu)^\\s*от\\s+рождения\\s+до\\s+(\\d+)\\s*мес\\p{L}*\\s*$");
    private static final Pattern AGE_RANGE = Pattern.compile("(?iu)^\\s*(\\d+)\\s*[-\\u2013\\u2014]\\s*(\\d+)\\s*мес\\p{L}*\\s*$");

    public static boolean isHeader(List<String> row) {
        String j = clean(String.join(" ", row)).toLowerCase(Locale.ROOT);
        return j.contains("возраст") && j.contains("дневного сна") && j.contains("ночной сон") && j.contains("бодрствования");
    }

    public static Parsed parse(List<List<String>> rows) {
        List<SleepRow> out = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        boolean headerSeen = false;
        for (List<String> raw : rows) {
            List<String> cells = raw.stream().map(SleepTableParser::clean).toList();
            if (!headerSeen) {
                headerSeen = isHeader(cells);
                continue;
            }
            if (cells.size() < 4 || cells.stream().allMatch(String::isEmpty)) continue;
            Optional<SleepRow> row = parseRow(cells);
            if (row.isPresent()) out.add(row.get());
            else skipped.add(String.join(" | ", cells));
        }
        return new Parsed(out, skipped);
    }

    static Optional<SleepRow> parseRow(List<String> c) {
        int from;
        int to;
        Matcher a = AGE_FROM_BIRTH.matcher(c.get(0));
        if (a.matches()) {
            from = 0;
            to = Integer.parseInt(a.group(1));
        } else {
            a = AGE_RANGE.matcher(c.get(0));
            if (!a.matches()) return Optional.empty();
            from = Integer.parseInt(a.group(1));
            to = Integer.parseInt(a.group(2));
        }
        if (to < from) return Optional.empty();

        Matcher naps = TIMES_BY.matcher(c.get(1));
        Matcher night = HOURS.matcher(c.get(2));
        if (!naps.matches() || !night.matches()) return Optional.empty();
        Matcher wake = TIMES_BY.matcher(c.get(3));
        boolean wakeNumeric = wake.matches();

        SleepBandData d = new SleepBandData(
                (int) num(naps, 1), (int) hi(naps, 1, 2),
                num(naps, 3), hi(naps, 3, 4),
                num(night, 1), hi(night, 1, 2),
                wakeNumeric ? (int) num(wake, 1) : null, wakeNumeric ? (int) hi(wake, 1, 2) : null,
                wakeNumeric ? num(wake, 3) : null, wakeNumeric ? hi(wake, 3, 4) : null,
                c.get(1), c.get(2), c.get(3));
        return Optional.of(new SleepRow(c.get(0), from, to, d));
    }

    private static double num(Matcher m, int g) {
        return Double.parseDouble(m.group(g).replace(',', '.'));
    }

    /** Верхняя граница диапазона; если записано одно число — оно же. */
    private static double hi(Matcher m, int lo, int hi) {
        return m.group(hi) != null ? num(m, hi) : num(m, lo);
    }

    static String clean(String s) {
        if (s == null) return "";
        return s.replace("\u00AD", "").replace("\u200B", "").replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }
}
