package org.example.tips_for_parents.service.pdf.util;

import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.pdf.NumericRange;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public final class NumberParsingUtils {

    private static final Pattern RANGE =
            Pattern.compile("^(\\d+(?:[.,]\\d+)?)(?:\\s*[–—-]\\s*(\\d+(?:[.,]\\d+)?))?$");

    public static NumericRange parseRange(String raw) {
        String s = clean(raw);
        if (s.isEmpty()) return null;
        if (s.matches("[-–—]+")) return NumericRange.ZERO;
        Matcher m = RANGE.matcher(s);
        if (!m.matches()) return null;
        double lo = d(m.group(1));
        return new NumericRange(lo, m.group(2) != null ? d(m.group(2)) : lo);
    }

    public static double d(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }

    public static String clean(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').replace("*", "").replaceAll("\\s+", " ").strip();
    }

    public static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    public static String fmt(NumericRange r) {
        return r.lo() == r.hi() ? fmt(r.lo()) : fmt(r.lo()) + "–" + fmt(r.hi());
    }
}
