package org.example.tips_for_parents.service.card;

import org.example.tips_for_parents.model.pdf.FoodNutrientProfile;
import org.example.tips_for_parents.model.value.NumericRange;
import org.example.tips_for_parents.model.value.Nutrients;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Чистые функции над количествами из таблицы 5.1: разбор, округление, отображение, пищевая ценность на единицу. */
final class PortionMath {
    private PortionMath() {
    }

    private static final Pattern FRACTION = Pattern.compile("^(\\d+)/(\\d+)$");

    static NumericRange parseAmount(String label, String rawAmount) {
        String a = NumberParsingUtils.clean(rawAmount);
        if (hasVariants(label, a)) a = a.split("/")[0].strip();
        Matcher f = FRACTION.matcher(a);
        if (f.matches()) return NumericRange.of(Double.parseDouble(f.group(1)) / Double.parseDouble(f.group(2)));
        return NumberParsingUtils.parseRange(a);
    }

    static boolean isFraction(String amount) {
        return FRACTION.matcher(NumberParsingUtils.clean(amount)).matches();
    }

    private static boolean hasVariants(String label, String amount) {
        return label.contains("/") && amount.contains("/") && label.split("/").length == amount.split("/").length;
    }

    static double roundAmount(double v, boolean fraction) {
        if (fraction) return Math.max(0.25, Math.round(v * 4) / 4.0);
        if (v >= 50) return Math.round(v / 5.0) * 5.0;
        return Math.max(1, Math.round(v));
    }

    static String display(double v, boolean fraction) {
        if (!fraction) return NumberParsingUtils.fmt(v);
        double q = Math.round(v * 4) / 4.0;
        if (q == 0.25) return "1/4";
        if (q == 0.5) return "1/2";
        if (q == 0.75) return "3/4";
        return NumberParsingUtils.fmt(q);
    }

    static String vsTable(double amount, NumericRange table) {
        if (amount < table.lo() - 1e-9) return "ниже таблицы 5.1";
        if (amount > table.hi() + 1e-9) return "выше таблицы 5.1";
        return "в пределах таблицы 5.1";
    }

    /** Середины диапазонов белка, жира и углеводов. */
    static double[] mid(Nutrients n) {
        return new double[]{(n.protein().lo() + n.protein().hi()) / 2, (n.fat().lo() + n.fat().hi()) / 2,
                (n.carbs().lo() + n.carbs().hi()) / 2};
    }

    /** Пищевая ценность на единицу количества блюда (по таблице 7.4); null, если блюда в таблице нет. */
    static Nutrients perUnit(String label, List<FoodNutrientProfile> nuts) {
        String a = ProductKeys.of(label.contains("/") ? label.split("/")[0] : label);
        Nutrients acc = null;
        for (var n : nuts) {
            String b = ProductKeys.of(n.name());
            if (!(a.startsWith(b) || b.startsWith(a))) continue;
            double k = 1.0 / n.basisAmount();
            Nutrients x = new Nutrients(n.energy().times(k), n.protein().times(k), n.fat().times(k), n.carbs().times(k));
            acc = acc == null ? x : acc.union(x);
        }
        return acc;
    }
}
