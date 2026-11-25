package org.example.tips_for_parents.model.personal;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.tips_for_parents.model.entity.ChildPlan;
import org.example.tips_for_parents.model.entity.ChildPlanDay;
import org.example.tips_for_parents.model.enums.PlanStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Персональный план: по месяцам — итоги (потребность, молоко, цель) и примерные дни (DayVariant как в карточке месяца). */
public record PlanView(UUID id, int version, PlanStatus status, String reason, Instant generatedAt,
                       List<MonthView> months) {

    public record MonthView(int month, JsonNode summary, List<DayView> days) {
    }

    public record DayView(int seq, String newProductCode, JsonNode day) {
    }

    public static PlanView of(ChildPlan p) {
        Map<Integer, List<DayView>> byMonth = p.getDays().stream().collect(Collectors.groupingBy(
                ChildPlanDay::getPlanMonth,
                Collectors.mapping(d -> new DayView(d.getSeq(), d.getNewProductCode(), d.getPayload()), Collectors.toList())));
        List<MonthView> months = p.getMonths().stream()
                .map(m -> new MonthView(m.getPlanMonth(), m.getSummary(), byMonth.getOrDefault(m.getPlanMonth(), List.of())))
                .toList();
        return new PlanView(p.getId(), p.getVersion(), p.getStatus(), p.getReason(), p.getGeneratedAt(), months);
    }
}
