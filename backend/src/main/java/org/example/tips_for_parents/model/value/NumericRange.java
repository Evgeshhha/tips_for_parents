package org.example.tips_for_parents.model.value;

public record NumericRange(double lo, double hi) {
    public static final NumericRange ZERO = new NumericRange(0, 0);
    public static NumericRange of(double v) {
        return new NumericRange(v, v);
    }
    public NumericRange plus(NumericRange o) {
        return new NumericRange(lo + o.lo, hi + o.hi);
    }
    public NumericRange times(double k) {
        return new NumericRange(lo * k, hi * k);
    }
    public NumericRange union(NumericRange o) {
        return new NumericRange(Math.min(lo, o.lo), Math.max(hi, o.hi));
    }
    public NumericRange minusClamped(NumericRange o) {
        return new NumericRange(Math.max(0, lo - o.hi), Math.max(0, hi - o.lo));
    }
    public NumericRange rounded() {
        return new NumericRange(round1(lo), round1(hi));
    }
    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
