package org.example.tips_for_parents.service.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.card.*;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.example.tips_for_parents.model.payload.*;
import org.example.tips_for_parents.model.pdf.FeedingAgeCell;
import org.example.tips_for_parents.model.pdf.FoodNutrientProfile;
import org.example.tips_for_parents.model.value.NumericRange;
import org.example.tips_for_parents.model.value.Nutrients;
import org.example.tips_for_parents.model.value.VarSolve;
import org.example.tips_for_parents.repository.FeedingFactRepository;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;
import org.example.tips_for_parents.service.pdf.util.TextNormalizationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MonthCardService {

    private static final class Entry {
        final Row row;
        final Entry parent;
        final NumericRange table;
        final boolean fraction;
        final Nutrients perUnit;
        double amount;
        String policy;
        Entry(Row row, Entry parent, NumericRange table, boolean fraction, Nutrients perUnit) {
            this.row = row; this.parent = parent; this.table = table; this.fraction = fraction; this.perUnit = perUnit;
        }
        double typical() {
            return table.lo() == table.hi() ? table.lo() : (table.lo() + table.hi()) / 2;
        }
    }

    private static final String POLICY_NEW = "новый продукт вводят постепенно — нижняя граница диапазона таблицы 5.1";
    private static final String POLICY_CALC = "рассчитано по БЖУ: остаток нормы после молока/смеси, не выше таблицы 5.1";
    private static final String POLICY_NO_DATA = "по таблице 5.1: пищевой ценности блюда нет в таблице 7.4";
    private static final String POLICY_TABLE = "по таблице 5.1 (нормы или вес не определены — расчёт невозможен)";

    private static final Pattern ADDON = Pattern.compile("(?iu)^добавляется к\\s+(.+)$");
    private static final Pattern FRACTION = Pattern.compile("^(\\d+)/(\\d+)$");
    private static final int MIN_MONTH = 4;
    private static final int MAX_MONTH = 12;
    private static final double DAYS_IN_MONTH = 30.4;
    private static final double LAMBDA = 0.05;
    private static final int SWEEPS = 500;

    private final FeedingFactRepository repo;
    private final ObjectMapper om;
    @Value("${cards.first-month:4}")
    private int firstMonth;
    @Value("${cards.last-month:12}")
    private int lastMonth;
    @Value("${cards.birth-weight-kg:3.3}")
    private double defaultBirthWeightKg;
    @Value("${cards.min-portion-share:0.25}")
    private double minPortionShare;

    public List<MonthCard> allCards(Double weightKg, Double birthWeightKg, String milk, Double milkMl) {
        return IntStream.rangeClosed(firstMonth, lastMonth)
                .mapToObj(m -> card(m, weightKg, birthWeightKg, milk, milkMl)).toList();
    }

    public MonthCard card(int month, Double weightKg, Double birthWeightKg, String milkKind, Double milkMl) {
        if (month < MIN_MONTH || month > MAX_MONTH)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Месяц должен быть от " + MIN_MONTH + " до " + MAX_MONTH);
        if (weightKg != null && (weightKg < 2 || weightKg > 20))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weightKg вне разумных пределов (2–20 кг)");
        String kind = milkKind == null || milkKind.isBlank() ? "breast" : milkKind.toLowerCase();
        if (!kind.equals("breast") && !kind.equals("formula"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milk: breast или formula");
        if (milkMl != null && (milkMl < 0 || milkMl > 2000))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milkMl вне разумных пределов (0–2000)");

        Map<String, List<FeedingFact>> byType = repo.findByReviewStatusOrderById(ReviewStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(FeedingFact::getFactType));
        List<String> warnings = new ArrayList<>();

        List<Row> rows = tableRows(month, byType.getOrDefault("TABLE_AGE", List.of()));
        if (rows.isEmpty())
            warnings.add("Нет утверждённых строк таблицы 5.1 для этого возраста — PDF не загружен или ждёт ревью");
        String column = rows.isEmpty() ? null : rows.getFirst().cell().age();
        List<AllowedItem> allowed = rows.stream()
                .map(r -> new AllowedItem(r.label(), r.cell().amount(), r.isNew(), r.notes(), r.fact().getId())).toList();
        List<String> newInPeriod = rows.stream().filter(r -> r.isNew() && r.addonStem() == null).map(Row::label).toList();

        Needs needs = needs(month, weightKg, birthWeightKg, byType, warnings);
        MilkInfo milk = needs == null ? null : milk(month, kind, milkMl, needs, byType, warnings);
        Nutrients target = needs == null ? null
                : milk == null ? needs.perDay() : needs.perDay().remainderAfter(milk.perDay());

        List<FoodNutrientProfile> nuts = byType.getOrDefault("NUTRIENT", List.of()).stream()
                .map(f -> read(f.getPayload(), FoodNutrientProfile.class)).filter(Objects::nonNull).toList();
        if (nuts.isEmpty()) warnings.add("Нет утверждённых данных таблицы 7.4 — БЖУ блюд неизвестно, расчёт граммовок невозможен");

        List<FeedingFact> optionalFacts = byType.getOrDefault("OPTIONAL", List.of());
        List<FeedingFact> subjectRules = byType.getOrDefault("RULE", List.of()).stream().filter(f -> f.getSubject() != null).toList();
        List<Row> mains = rows.stream().filter(r -> r.addonStem() == null).toList();
        List<Row> dayRows = mains.stream().filter(r -> optionalFor(r, optionalFacts) == null).toList();
        List<TextFact> optional = mains.stream().filter(r -> optionalFor(r, optionalFacts) != null)
                .map(r -> new TextFact(r.label() + " — не входит в примерный день. Программа: " + optionalFor(r, optionalFacts).getSentence(),
                        optionalFor(r, optionalFacts).getId())).toList();

        FeedingFact sched = byType.getOrDefault("SCHEDULE", List.of()).stream()
                .filter(s -> s.getValueMin() != null && month >= s.getValueMin()).findFirst().orElse(null);
        SchedulePayload sp = sched == null ? null : read(sched.getPayload(), SchedulePayload.class);

        List<Row> established = dayRows.stream().filter(r -> !r.isNew()).toList();
        List<Row> newRows = dayRows.stream().filter(Row::isNew).toList();
        Nutrients milkDay = milk == null ? null : milk.perDay();

        List<DayVariant> day = new ArrayList<>();
        if (established.isEmpty()) {
            for (Row x : dayRows) {
                String firstNote = x.notes().stream().filter(n -> n.toLowerCase().contains("первого")).findFirst()
                        .map(n -> "Таблица 5.1: «" + n + "» — этот день применим, если ребёнок уже получает другой прикорм")
                        .orElse(null);
                day.add(variant("Новый продукт: " + x.label(), x.label(), firstNote, List.of(x), x, rows, nuts, needs,
                        target, milkDay, subjectRules, true));
            }
        } else if (!newRows.isEmpty()) {
            for (Row x : newRows) {                                   // по одному новому продукту на день
                List<Row> dishes = new ArrayList<>(established);
                Row replaced = null;
                if (sched != null && sp != null && TextNormalizationUtils.matches(x.label(), sched.getSubject()))
                    replaced = findRow(dishes, sp.replaces());          // рыба вводится вместо мясного блюда
                if (replaced != null) dishes.remove(replaced);
                dishes.add(x);
                String note = replaced == null ? null : "Программа: " + sched.getSentence();
                day.add(variant("Новый продукт: " + x.label(), x.label(), note, dishes, x, rows, nuts, needs, target,
                        milkDay, subjectRules, false));
            }
        } else {
            Row fish = sched == null ? null : findRow(established, sched.getSubject());
            Row meat = sched == null || sp == null ? null : findRow(established, sp.replaces());
            if (fish != null && meat != null) {
                final Row f = fish;
                final Row m = meat;
                day.add(variant("День без новых продуктов, с мясным блюдом", null, null,
                        established.stream().filter(r -> r != f).toList(), null, rows, nuts, needs, target, milkDay, subjectRules, false));
                day.add(variant("День без новых продуктов, с рыбным блюдом", null, "Программа: " + sched.getSentence(),
                        established.stream().filter(r -> r != m).toList(), null, rows, nuts, needs, target, milkDay, subjectRules, false));
            } else {
                day.add(variant("День без новых продуктов", null, null, established, null, rows, nuts, needs, target,
                        milkDay, subjectRules, false));
            }
        }

        List<TextFact> rules = byType.getOrDefault("RULE", List.of()).stream()
                .filter(f -> f.getSubject() == null && (f.getValueMin() == null || month >= f.getValueMin()))
                .map(f -> new TextFact(f.getSentence(), f.getId())).toList();
        if (rules.isEmpty()) warnings.add("Нет утверждённых правил из PDF (проверьте GET /api/admin/status)");
        List<TextFact> restrictions = byType.getOrDefault("RESTRICTION", List.of()).stream()
                .map(f -> new TextFact(f.getSentence(), f.getId())).toList();

        List<String> notes = new ArrayList<>(List.of(
                "Количества в таблице 5.1 — г, мл (желток — доли штуки); в источнике не указано, на одно кормление они или на сутки.",
                "Время кормлений и число приёмов пищи в программе не заданы, поэтому «примерный день» — набор блюд без привязки ко времени.",
                "В PDF не найден общий перечень запрещённых продуктов (мёд, твёрдые круглые продукты, сырые мясо и рыба и т. п.); "
                        + "в restrictions — только ограничения, прямо указанные в тексте программы."));
        notes.add("Примерные дни независимы друг от друга: это не последовательность и не приоритет, а примеры дней, "
                + "которые можно использовать в этом месяце (в каждом — один новый продукт). Порядок введения продуктов в программе не задан.");
        if (!dayRows.isEmpty() && established.isEmpty())
            notes.add("Первый столбец таблицы 5.1 (начало прикорма): в день один новый продукт в стартовом количестве "
                    + "(нижняя граница диапазона), основную часть питания по-прежнему даёт молоко/смесь — "
                    + "расчёт по верхней границе объёма молока может показывать недобор энергии за день.");
        if (needs != null) {
            notes.add("Норма белка в приложении 1 дана для детей на искусственном вскармливании (сноска таблицы).");
            notes.add("Граммовки привычных блюд рассчитаны по БЖУ так, чтобы прикорм покрывал остаток нормы после молока/смеси; "
                    + "верхняя граница каждого блюда — значение таблицы 5.1. Поле vsTable показывает, где расчёт относительно таблицы.");
        }
        if (milk != null)
            notes.add("Объём молока/смеси — верхняя граница из программы (в программе нет типичного объёма по месяцам). "
                    + "Реальный объём ниже по мере роста прикорма, тогда прикорма нужно больше: передайте milkMl.");

        Needs dto = needs == null ? null : new Needs(needs.ageBand(), Math.round(needs.weightKg() * 100) / 100.0,
                needs.weightSource(), needs.perKg(), needs.perDay().rounded());
        MilkInfo milkDto = milk == null ? null : new MilkInfo(milk.kind(), milk.label(), Math.round(milk.volumeMl()),
                milk.volumeSource(), milk.per100ml(), milk.perDay().rounded(), milk.compositionSource());
        return new MonthCard(month, column, newInPeriod, allowed, dto, milkDto, target == null ? null : target.rounded(),
                day, optional, rules, restrictions, notes, warnings);
    }

    private List<Row> tableRows(int month, List<FeedingFact> tableFacts) {
        List<Row> rows = new ArrayList<>();
        for (FeedingFact f : tableFacts) {
            TablePayload pl = read(f.getPayload(), TablePayload.class);
            if (pl == null || pl.cells() == null || pl.cells().isEmpty()) continue;
            FeedingAgeCell cell = pl.cells().stream()
                    .filter(c -> c.lo() <= month && month <= c.hi()).findFirst().orElse(null);
            if (cell == null) continue;                       // в этом возрасте продукт не вводится

            String addonStem = null;
            List<String> notes = new ArrayList<>();
            if (pl.footnotes() != null) {
                for (String fn : pl.footnotes()) {
                    Matcher m = ADDON.matcher(fn);
                    if (m.matches()) addonStem = TextNormalizationUtils.of(TextNormalizationUtils.firstWord(m.group(1)));   // «овощному пюре» -> «овощ»
                    else notes.add(fn);
                }
            }
            rows.add(new Row(f, cell, pl.unit(), cell.lo() == pl.cells().getFirst().lo(), addonStem, notes));
        }
        return rows;
    }

    private Needs needs(int month, Double weightKg, Double birthWeightKg,
                        Map<String, List<FeedingFact>> byType, List<String> warnings) {
        FeedingFact norm = byType.getOrDefault("NORM", List.of()).stream()
                .filter(f -> f.getValueMin() != null && f.getValueMax() != null && f.getValueMin() <= month && month <= f.getValueMax())
                .findFirst().orElse(null);
        NormPayload np = norm == null ? null : read(norm.getPayload(), NormPayload.class);
        if (np == null) {
            warnings.add("Нет утверждённых норм (приложение 1) для этого возраста — потребность не считается");
            return null;
        }
        Weight w = weight(month, weightKg, birthWeightKg, byType.getOrDefault("WEIGHT_GAIN", List.of()));
        if (w == null) {
            warnings.add("Масса ребёнка неизвестна: передайте weightKg или загрузите таблицу 6.1 — потребность не считается");
            return null;
        }
        Nutrients perKg = Nutrients.exact(np.energy(), np.protein(), np.fat(), np.carbs());
        return new Needs(norm.getValueText(), w.kg(), w.source(), perKg, perKg.times(w.kg()));
    }

    private Weight weight(int month, Double weightKg, Double birthWeightKg, List<FeedingFact> gains) {
        if (weightKg != null) return new Weight(weightKg, "задана в запросе");
        if (gains.isEmpty()) return null;
        double birth = birthWeightKg != null ? birthWeightKg : defaultBirthWeightKg;
        double kg = birth;
        for (int k = 0; k < month; k++) {
            final int kk = k;
            FeedingFact band = gains.stream()
                    .filter(g -> g.getValueMin() != null && g.getValueMax() != null && g.getValueMin() <= kk && kk < g.getValueMax())
                    .findFirst().orElse(null);
            GainPayload gp = band == null ? null : read(band.getPayload(), GainPayload.class);
            if (gp == null) break;
            kg += (gp.gramsPerDay().lo() + gp.gramsPerDay().hi()) / 2 * DAYS_IN_MONTH / 1000;
        }
        return new Weight(kg, "оценка по средним прибавкам таблицы 6.1 при массе при рождении "
                + NumberParsingUtils.fmt(birth) + " кг (масса при рождении — не из программы, можно передать birthWeightKg)");
    }

    private MilkInfo milk(int month, String kind, Double milkMl, Needs needs,
                          Map<String, List<FeedingFact>> byType, List<String> warnings) {
        FeedingFact comp = byType.getOrDefault("MILK_COMPOSITION", List.of()).stream()
                .filter(f -> kind.equals(f.getSubject())).findFirst().orElse(null);
        MilkPayload mp = comp == null ? null : read(comp.getPayload(), MilkPayload.class);
        if (mp == null) {
            warnings.add("Нет утверждённого состава молока/смеси («" + kind + "») — молоко не вычитается из нормы");
            return null;
        }
        Nutrients per100 = Nutrients.exact(mp.energy(), mp.protein(), mp.fat(), mp.carbs());

        double volume;
        String source;
        if (milkMl != null) {
            volume = milkMl;
            source = "задан в запросе (milkMl)";
        } else {
            FeedingFact cap = byType.getOrDefault("MILK_VOLUME", List.of()).stream()
                    .filter(f -> f.getValueMin() != null && f.getValueMin() <= month && (f.getValueMax() == null || month <= f.getValueMax()))
                    .findFirst().orElse(null);
            VolumePayload vp = cap == null ? null : read(cap.getPayload(), VolumePayload.class);
            if (vp == null) {
                warnings.add("Объём молока/смеси для этого возраста в программе не указан — передайте milkMl; молоко не вычитается");
                return null;
            }
            double byEnergy = needs.perDay().energy().lo() / (mp.energy() / 100);   // калорийный метод
            volume = Math.min(byEnergy, vp.maxMl());
            source = (volume < vp.maxMl() ? "по калорийному методу: " : "верхняя граница программы (" + vp.maxMl() + " мл/сут): ")
                    + cap.getSentence();
        }
        return new MilkInfo(kind, comp.getValueText(), volume, source, per100, per100.times(volume / 100), comp.getSentence());
    }

    private DayVariant variant(String title, String newProduct, String note, List<Row> dishRows, Row newRow,
                               List<Row> allRows, List<FoodNutrientProfile> nuts, Needs needs,
                               Nutrients target, Nutrients milkDay, List<FeedingFact> subjectRules,
                               boolean startOfFeeding) {
        List<Entry> entries = new ArrayList<>();
        List<String> noData = new ArrayList<>();
        for (Row r : dishRows) {
            Entry main = entry(r, null, nuts);
            if (main == null) { noData.add(r.label() + " (количество не разобрано)"); continue; }
            entries.add(main);
            for (Row a : allRows) {
                if (a.addonStem() == null || !TextNormalizationUtils.matches(r.label(), a.addonStem())) continue;
                Entry add = entry(a, main, nuts);
                if (add != null) entries.add(add);
            }
        }

        boolean calc = target != null;
        double[] fixed = new double[3];
        List<VarSolve> varSolves = new ArrayList<>();
        List<Entry> varEntries = new ArrayList<>();
        for (Entry e : entries) {
            if (e.row == newRow || startOfFeeding) { e.amount = e.table.lo(); e.policy = POLICY_NEW; }
            else if (e.perUnit == null) { e.amount = e.typical(); e.policy = POLICY_NO_DATA; }
            else if (!calc) { e.amount = e.typical(); e.policy = POLICY_TABLE; }
            else {
                varSolves.add(new VarSolve(mid(e.perUnit), minPortionShare * e.table.hi(), e.table.hi(), e.typical()));
                varEntries.add(e);
                continue;
            }
            if (e.perUnit != null) {
                double[] d = mid(e.perUnit);
                for (int k = 0; k < 3; k++) fixed[k] += d[k] * e.amount;
            }
        }
        if (!varSolves.isEmpty()) {
            double[] t = {Math.max(target.protein().lo() - fixed[0], 0), Math.max(target.fat().lo() - fixed[1], 0),
                    Math.max(target.carbs().lo() - fixed[2], 0)};
            double[] x = DaySolver.solve(varSolves, t, LAMBDA, SWEEPS);
            for (int i = 0; i < x.length; i++) {
                Entry e = varEntries.get(i);
                VarSolve v = varSolves.get(i);
                double r = roundAmount(x[i], e.fraction);
                e.amount = Math.min(v.hi(), Math.max(Math.min(v.lo(), v.hi()), r));
                e.policy = POLICY_CALC;
            }
        }

        Nutrients totals = Nutrients.ZERO;
        List<DayDish> dishes = new ArrayList<>();
        for (Entry e : entries) {
            if (e.perUnit != null) totals = totals.plus(e.perUnit.times(e.amount));
            else noData.add(e.row.label());
        }
        for (Entry e : entries) {
            if (e.parent != null) continue;
            List<Item> addons = entries.stream().filter(a -> a.parent == e)
                    .map(a -> new Item(a.row.label(), display(a.amount, a.fraction), unit(a),
                            a.perUnit == null ? null : a.perUnit.times(a.amount).rounded(), a.row.fact().getId())).toList();
            List<Hint> hints = subjectRules.stream().filter(s -> TextNormalizationUtils.matches(e.row.label(), s.getSubject()))
                    .map(s -> new Hint(s.getSentence(), s.getId())).toList();
            dishes.add(new DayDish(e.row.label(), display(e.amount, e.fraction), unit(e), e.row.cell().amount(),
                    vsTable(e.amount, e.table), e.policy, e.perUnit == null ? null : e.perUnit.times(e.amount).rounded(),
                    addons, hints, e.row.fact().getId()));
        }

        Nutrients pctTarget = target == null ? null : totals.percentOf(target).rounded();
        Nutrients dayTotals = milkDay == null ? null : totals.plus(milkDay).rounded();
        Nutrients pctNeed = needs == null || milkDay == null ? null : totals.plus(milkDay).percentOf(needs.perDay()).rounded();
        return new DayVariant(title, newProduct, note, dishes, totals.rounded(), pctTarget, dayTotals, pctNeed,
                noData.isEmpty() ? List.of() : List.of("Нет данных о пищевой ценности в программе (таблица 7.4): "
                        + String.join(", ", noData)));
    }

    private Entry entry(Row r, Entry parent, List<FoodNutrientProfile> nuts) {
        NumericRange range = parseAmount(r.label(), r.cell().amount());
        if (range == null) return null;
        return new Entry(r, parent, range, isFraction(r.cell().amount()), perUnit(r.label(), nuts));
    }

    private static double[] mid(Nutrients n) {
        return new double[]{(n.protein().lo() + n.protein().hi()) / 2, (n.fat().lo() + n.fat().hi()) / 2,
                (n.carbs().lo() + n.carbs().hi()) / 2};
    }

    private static String unit(Entry e) {
        return e.fraction ? "шт." : (e.row.unit() == null || e.row.unit().isBlank() ? "г, мл" : e.row.unit());
    }

    private static Row findRow(List<Row> rows, String stem) {
        return rows.stream().filter(r -> TextNormalizationUtils.matches(r.label(), stem)).findFirst().orElse(null);
    }

    private static FeedingFact optionalFor(Row r, List<FeedingFact> optionalFacts) {
        return optionalFacts.stream().filter(o -> TextNormalizationUtils.matches(r.label(), o.getSubject())).findFirst().orElse(null);
    }


    private static boolean hasVariants(String label, String amount) {
        return label.contains("/") && amount.contains("/") && label.split("/").length == amount.split("/").length;
    }

    private static boolean isFraction(String amount) {
        return FRACTION.matcher(NumberParsingUtils.clean(amount)).matches();
    }

    private static NumericRange parseAmount(String label, String rawAmount) {
        String a = NumberParsingUtils.clean(rawAmount);
        if (hasVariants(label, a)) a = a.split("/")[0].strip();
        Matcher f = FRACTION.matcher(a);
        if (f.matches()) return NumericRange.of(Double.parseDouble(f.group(1)) / Double.parseDouble(f.group(2)));
        return NumberParsingUtils.parseRange(a);
    }

    private static double roundAmount(double v, boolean fraction) {
        if (fraction) return Math.max(0.25, Math.round(v * 4) / 4.0);
        if (v >= 50) return Math.round(v / 5.0) * 5.0;
        return Math.max(1, Math.round(v));
    }

    private static String display(double v, boolean fraction) {
        if (!fraction) return NumberParsingUtils.fmt(v);
        double q = Math.round(v * 4) / 4.0;
        if (q == 0.25) return "1/4";
        if (q == 0.5) return "1/2";
        if (q == 0.75) return "3/4";
        return NumberParsingUtils.fmt(q);
    }

    private static String vsTable(double amount, NumericRange table) {
        if (amount < table.lo() - 1e-9) return "ниже таблицы 5.1";
        if (amount > table.hi() + 1e-9) return "выше таблицы 5.1";
        return "в пределах таблицы 5.1";
    }

    private static Nutrients perUnit(String label, List<FoodNutrientProfile> nuts) {
        String a = norm(label.contains("/") ? label.split("/")[0] : label);
        Nutrients acc = null;
        for (var n : nuts) {
            String b = norm(n.name());
            if (!(a.startsWith(b) || b.startsWith(a))) continue;
            double k = 1.0 / n.basisAmount();
            Nutrients x = new Nutrients(n.energy().times(k), n.protein().times(k), n.fat().times(k), n.carbs().times(k));
            acc = acc == null ? x : acc.union(x);
        }
        return acc;
    }

    private static String norm(String s) { return NumberParsingUtils.clean(s).toLowerCase(); }

    private <T> T read(String json, Class<T> type) {
        if (json == null) return null;
        try {
            return om.readValue(json, type);
        } catch (IOException e) {
            return null;
        }
    }
}
