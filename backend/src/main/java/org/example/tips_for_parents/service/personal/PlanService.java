package org.example.tips_for_parents.service.personal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.config.CardsProperties;
import org.example.tips_for_parents.exception.ApiErrors;
import org.example.tips_for_parents.model.card.ChildContext;
import org.example.tips_for_parents.model.card.DayVariant;
import org.example.tips_for_parents.model.card.MonthCard;
import org.example.tips_for_parents.model.entity.*;
import org.example.tips_for_parents.model.enums.PlanStatus;
import org.example.tips_for_parents.model.personal.PlanView;
import org.example.tips_for_parents.repository.*;
import org.example.tips_for_parents.service.card.MonthCardService;
import org.example.tips_for_parents.service.card.ProductKeys;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Строит и сохраняет персональный план: для каждого месяца от текущего возраста ребёнка до cards.last-month
 * вызывает существующий MonthCardService с учётом статусов продуктов и массы ребёнка.
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final ChildRepository children;
    private final ChildProductRepository products;
    private final WeightMeasurementRepository weights;
    private final ChildPlanRepository plans;
    private final MonthCardService cards;
    private final CardsProperties props;
    private final ObjectMapper om;

    /** Новая версия плана; прежняя активная получает SUPERSEDED (история сохраняется). */
    @Transactional
    public PlanView regenerate(UUID childId, String reason) {
        Child child = children.getOrThrow(childId);

        int startMonth = Ages.clamp(Ages.months(child.getBirthDate(), LocalDate.now()), props.firstMonth(), props.lastMonth());

        Set<String> introduced = new HashSet<>();
        Set<String> pending = new HashSet<>();
        Set<String> unavailable = new HashSet<>();
        for (ChildProduct cp : products.findByChildId(childId)) {
            switch (cp.getStatus()) {
                case INTRODUCED -> introduced.add(cp.getProductCode());
                case NOT_STARTED, INTRODUCING -> pending.add(cp.getProductCode());
                case PAUSED, EXCLUDED -> unavailable.add(cp.getProductCode());
            }
        }
        ChildContext ctx = new ChildContext(Set.copyOf(introduced), Set.copyOf(pending), Set.copyOf(unavailable));

        Double currentWeight = weights.findFirstByChildIdOrderByMeasuredOnDescIdDesc(childId)
                .map(WeightMeasurement::getWeightKg).orElse(null);
        String milk = child.getFeedingType().name().toLowerCase(Locale.ROOT);          // breast | formula
        Double milkMl = child.getMilkMl() == null ? null : child.getMilkMl().doubleValue();

        plans.findByChildIdAndStatus(childId, PlanStatus.ACTIVE).forEach(p -> p.setStatus(PlanStatus.SUPERSEDED));
        int version = plans.findFirstByChildIdOrderByVersionDesc(childId).map(p -> p.getVersion() + 1).orElse(1);

        ChildPlan plan = new ChildPlan(child, version, reason);
        for (int month = startMonth; month <= props.lastMonth(); month++) {
            // реальная масса известна на сегодня; для будущих месяцев алгоритм оценит массу по таблице 6.1
            Double weight = month == startMonth ? currentWeight : null;
            MonthCard card = cards.card(month, weight, child.getBirthWeightKg(), milk, milkMl, ctx);

            plan.addMonth(new ChildPlanMonth(month, summary(card)));
            int seq = 0;
            for (DayVariant v : card.exampleDay()) {
                String key = v.newProduct() == null ? null : ProductKeys.of(v.newProduct());
                plan.addDay(new ChildPlanDay(month, seq++, key, om.valueToTree(v)));
            }
        }
        return PlanView.of(plans.save(plan));
    }

    @Transactional(readOnly = true)
    public PlanView getActive(UUID childId) {
        return plans.findFirstByChildIdAndStatus(childId, PlanStatus.ACTIVE)
                .map(PlanView::of)
                .orElseThrow(() -> ApiErrors.notFound("У ребёнка пока нет плана"));
    }

    private ObjectNode summary(MonthCard card) {
        ObjectNode n = om.createObjectNode();
        n.set("column", om.valueToTree(card.column()));
        n.set("needs", om.valueToTree(card.needs()));
        n.set("milk", om.valueToTree(card.milk()));
        n.set("prikormTarget", om.valueToTree(card.prikormTarget()));
        n.set("warnings", om.valueToTree(card.warnings()));
        return n;
    }
}
