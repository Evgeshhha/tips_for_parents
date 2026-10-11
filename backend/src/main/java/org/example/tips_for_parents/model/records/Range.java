package org.example.tips_for_parents.model.records;

/** Числовой диапазон (в таблицах программы многие значения — «от–до»). Одно значение = lo == hi. */
public record Range(double lo, double hi) {

    public static final Range ZERO = new Range(0, 0);

    public static Range of(double v) { return new Range(v, v); }

    public Range plus(Range o) { return new Range(lo + o.lo, hi + o.hi); }

    public Range times(double k) { return new Range(lo * k, hi * k); }

    public Range union(Range o) { return new Range(Math.min(lo, o.lo), Math.max(hi, o.hi)); }

    /** this − other, не ниже нуля (остаток потребности). */
    public Range minusClamped(Range o) { return new Range(Math.max(0, lo - o.hi), Math.max(0, hi - o.lo)); }

    public Range rounded() { return new Range(round1(lo), round1(hi)); }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
