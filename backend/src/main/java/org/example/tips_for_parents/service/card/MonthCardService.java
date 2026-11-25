package org.example.tips_for_parents.service.card;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.config.CardsProperties;
import org.example.tips_for_parents.exception.ApiErrors;
import org.example.tips_for_parents.model.card.*;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.FactType;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.example.tips_for_parents.model.payload.SchedulePayload;
import org.example.tips_for_parents.model.pdf.FoodNutrientProfile;
import org.example.tips_for_parents.model.value.Nutrients;
import org.example.tips_for_parents.repository.FeedingFactRepository;
import org.example.tips_for_parents.service.common.JsonSupport;
import org.example.tips_for_parents.service.common.MonthValidator;
import org.example.tips_for_parents.service.pdf.util.TextNormalizationUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
@Service
@RequiredArgsConstructor
public class MonthCardService {

    private static final int MIN_MONTH = 4;
    private static final int MAX_MONTH = 12;

    private final FeedingFactRepository repo;
    private final JsonSupport json;
    private final CardsProperties props;
    private final FeedingTableReader tableReader;
    private final NeedsCalculator needsCalculator;
    private final MilkCalculator milkCalculator;
    private final ExampleDayPlanner dayPlanner;

    public List<MonthCard> allCards(Double weightKg, Double birthWeightKg, String milk, Double milkMl) {
        return IntStream.rangeClosed(props.firstMonth(), props.lastMonth())
                .mapToObj(m -> card(m, weightKg, birthWeightKg, milk, milkMl)).toList();
    }

    public MonthCard card(int month, Double weightKg, Double birthWeightKg, String milkKind, Double milkMl) {
        return card(month, weightKg, birthWeightKg, milkKind, milkMl, ChildContext.NONE);
    }

    public MonthCard card(int month, Double weightKg, Double birthWeightKg, String milkKind, Double milkMl,
                          ChildContext child) {
        MonthValidator.require(month, MIN_MONTH, MAX_MONTH);
        if (weightKg != null && (weightKg < 2 || weightKg > 20))
            throw ApiErrors.badRequest("weightKg вне разумных пределов (2–20 кг)");
        String kind = milkKind == null || milkKind.isBlank() ? "breast" : milkKind.toLowerCase();
        if (!kind.equals("breast") && !kind.equals("formula"))
            throw ApiErrors.badRequest("milk: breast или formula");
        if (milkMl != null && (milkMl < 0 || milkMl > 2000))
            throw ApiErrors.badRequest("milkMl вне разумных пределов (0–2000)");

        ApprovedFacts facts = ApprovedFacts.from(repo.findByReviewStatusOrderById(ReviewStatus.APPROVED));
        List<String> warnings = new ArrayList<>();

        List<Row> rows = tableReader.rows(month, facts.of(FactType.TABLE_AGE));
        if (rows.isEmpty())
            warnings.add("Нет утверждённых строк таблицы 5.1 для этого возраста — PDF не загружен или ждёт ревью");
        String column = rows.isEmpty() ? null : rows.getFirst().cell().age();
        List<AllowedItem> allowed = rows.stream()
                .map(r -> new AllowedItem(r.label(), r.cell().amount(), r.isNew(), r.notes(), r.fact().getId())).toList();
        List<String> newInPeriod = rows.stream().filter(r -> r.isNew() && r.addonStem() == null).map(Row::label).toList();

        List<String> skipped = rows.stream()
                .filter(r -> r.addonStem() == null && child.unavailable().contains(ProductKeys.of(r.label())))
                .map(Row::label).toList();
        if (!skipped.isEmpty())
            warnings.add("Исключены из примерного дня по данным ребёнка (пауза или исключён): " + String.join(", ", skipped));

        Needs needs = needsCalculator.calculate(month, weightKg, birthWeightKg, facts, warnings);
        MilkInfo milk = needs == null ? null : milkCalculator.calculate(month, kind, milkMl, needs, facts, warnings);
        Nutrients target = needs == null ? null
                : milk == null ? needs.perDay() : needs.perDay().remainderAfter(milk.perDay());

        List<FoodNutrientProfile> nuts = facts.of(FactType.NUTRIENT).stream()
                .map(f -> json.readOrNull(f.getPayload(), FoodNutrientProfile.class)).filter(Objects::nonNull).toList();
        if (nuts.isEmpty()) warnings.add("Нет утверждённых данных таблицы 7.4 — БЖУ блюд неизвестно, расчёт граммовок невозможен");

        List<FeedingFact> optionalFacts = facts.of(FactType.OPTIONAL);
        List<FeedingFact> subjectRules = facts.of(FactType.RULE).stream().filter(f -> f.getSubject() != null).toList();
        List<Row> usableRows = rows.stream().filter(r -> !child.unavailable().contains(ProductKeys.of(r.label()))).toList();
        List<Row> mains = usableRows.stream().filter(r -> r.addonStem() == null).toList();
        List<Row> dayRows = mains.stream().filter(r -> optionalFor(r, optionalFacts) == null).toList();
        List<TextFact> optional = mains.stream().filter(r -> optionalFor(r, optionalFacts) != null)
                .map(r -> new TextFact(r.label() + " — не входит в примерный день. Программа: " + optionalFor(r, optionalFacts).getSentence(),
                        optionalFor(r, optionalFacts).getId())).toList();

        FeedingFact sched = facts.of(FactType.SCHEDULE).stream()
                .filter(s -> s.getValueMin() != null && month >= s.getValueMin()).findFirst().orElse(null);
        SchedulePayload sp = sched == null ? null : json.readOrNull(sched.getPayload(), SchedulePayload.class);

        Nutrients milkDay = milk == null ? null : milk.perDay();
        DayContext ctx = new DayContext(usableRows, nuts, needs, target, milkDay, subjectRules);
        List<DayVariant> day = dayPlanner.plan(ctx, dayRows, child, sched, sp);

        List<TextFact> rules = facts.of(FactType.RULE).stream()
                .filter(f -> f.getSubject() == null && (f.getValueMin() == null || month >= f.getValueMin()))
                .map(f -> new TextFact(f.getSentence(), f.getId())).toList();
        if (rules.isEmpty()) warnings.add("Нет утверждённых правил из PDF (проверьте GET /api/admin/status)");
        List<TextFact> restrictions = facts.of(FactType.RESTRICTION).stream()
                .map(f -> new TextFact(f.getSentence(), f.getId())).toList();

        boolean onlyFirstFeedingDays = !dayRows.isEmpty() && dayRows.stream().allMatch(r -> ExampleDayPlanner.isNewFor(r, child));
        List<String> notes = CardNotes.build(onlyFirstFeedingDays, needs != null, milk != null);

        Needs needsDto = needs == null ? null : new Needs(needs.ageBand(), Math.round(needs.weightKg() * 100) / 100.0,
                needs.weightSource(), needs.perKg(), needs.perDay().rounded());
        MilkInfo milkDto = milk == null ? null : new MilkInfo(milk.kind(), milk.label(), Math.round(milk.volumeMl()),
                milk.volumeSource(), milk.per100ml(), milk.perDay().rounded(), milk.compositionSource());
        return new MonthCard(month, column, newInPeriod, allowed, needsDto, milkDto, target == null ? null : target.rounded(),
                day, optional, rules, restrictions, notes, warnings);
    }

    /** Продукты таблицы 5.1 из утверждённых фактов (без добавок вроде масла): ключ -> название. */
    public Map<String, String> productCatalog() {
        ApprovedFacts facts = ApprovedFacts.from(repo.findByReviewStatusOrderById(ReviewStatus.APPROVED));
        return tableReader.catalog(facts.of(FactType.TABLE_AGE));
    }

    private static FeedingFact optionalFor(Row r, List<FeedingFact> optionalFacts) {
        return optionalFacts.stream()
                .filter(o -> TextNormalizationUtils.matches(r.label(), o.getSubject()))
                .findFirst().orElse(null);
    }
}
