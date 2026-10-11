package org.example.tips_for_parents.service.pdf;

import org.example.tips_for_parents.model.records.Range;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбор чисел из таблиц программы: «8,5», «6,5–12», «-». */
public final class NumberParser {

    private static final Pattern RANGE =
            Pattern.compile("^(\\d+(?:[.,]\\d+)?)(?:\\s*[–—-]\\s*(\\d+(?:[.,]\\d+)?))?$");

    private NumberParser() {}

    /** @return диапазон; «-» = ноль; null — если разобрать нельзя. */
    public static Range parseRange(String raw) {
        String s = clean(raw);
        if (s.isEmpty()) return null;
        if (s.matches("[-–—]+")) return Range.ZERO;
        Matcher m = RANGE.matcher(s);
        if (!m.matches()) return null;
        double lo = d(m.group(1));
        return new Range(lo, m.group(2) != null ? d(m.group(2)) : lo);
    }

    public static double d(String s) { return Double.parseDouble(s.replace(',', '.')); }

    public static String clean(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').replace("*", "").replaceAll("\\s+", " ").strip();
    }

    /** 2.5 -> «2,5», 3.0 -> «3» (как в таблицах). */
    public static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    public static String fmt(Range r) {
        return r.lo() == r.hi() ? fmt(r.lo()) : fmt(r.lo()) + "–" + fmt(r.hi());
    }
}
