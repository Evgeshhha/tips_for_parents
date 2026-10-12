package org.example.tips_for_parents.model.value;

public record Nutrients(NumericRange energy, NumericRange protein, NumericRange fat, NumericRange carbs) {

    public static final Nutrients ZERO = new Nutrients(NumericRange.ZERO, NumericRange.ZERO, NumericRange.ZERO, NumericRange.ZERO);

    public static Nutrients exact(double energy, double protein, double fat, double carbs) {
        return new Nutrients(NumericRange.of(energy), NumericRange.of(protein), NumericRange.of(fat), NumericRange.of(carbs));
    }

    public Nutrients plus(Nutrients o) {
        return new Nutrients(energy.plus(o.energy), protein.plus(o.protein), fat.plus(o.fat), carbs.plus(o.carbs));
    }

    public Nutrients times(double k) {
        return new Nutrients(energy.times(k), protein.times(k), fat.times(k), carbs.times(k));
    }

    public Nutrients union(Nutrients o) {
        return new Nutrients(energy.union(o.energy), protein.union(o.protein), fat.union(o.fat), carbs.union(o.carbs));
    }

    public Nutrients remainderAfter(Nutrients eaten) {
        return new Nutrients(energy.minusClamped(eaten.energy), protein.minusClamped(eaten.protein),
                fat.minusClamped(eaten.fat), carbs.minusClamped(eaten.carbs));
    }

    public Nutrients percentOf(Nutrients need) {
        return new Nutrients(pct(energy, need.energy), pct(protein, need.protein),
                pct(fat, need.fat), pct(carbs, need.carbs));
    }

    public Nutrients rounded() {
        return new Nutrients(energy.rounded(), protein.rounded(), fat.rounded(), carbs.rounded());
    }

    private static NumericRange pct(NumericRange v, NumericRange need) {
        return need.lo() > 0 ? new NumericRange(v.lo() / need.lo() * 100, v.hi() / need.lo() * 100) : NumericRange.ZERO;
    }
}
