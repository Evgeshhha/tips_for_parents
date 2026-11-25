package org.example.tips_for_parents.service.card;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.card.ChildContext;
import org.example.tips_for_parents.model.card.DayVariant;
import org.example.tips_for_parents.model.card.Row;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.payload.SchedulePayload;
import org.example.tips_for_parents.service.pdf.util.TextNormalizationUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Решает, какие варианты «примерного дня» показать в месяце: по одному новому продукту на день или дни без новых. */
@Component
@RequiredArgsConstructor
public class ExampleDayPlanner {

    private final DayVariantBuilder builder;

    /** «Новый» для ребёнка: статус INTRODUCED -> привычный; NOT_STARTED/INTRODUCING -> новый; иначе по таблице 5.1. */
    static boolean isNewFor(Row r, ChildContext child) {
        String key = ProductKeys.of(r.label());
        if (child.introduced().contains(key)) return false;
        if (child.pending().contains(key)) return true;
        return r.isNew();
    }

    static Row findRow(List<Row> rows, String stem) {
        return rows.stream().filter(r -> TextNormalizationUtils.matches(r.label(), stem)).findFirst().orElse(null);
    }

    List<DayVariant> plan(DayContext ctx, List<Row> dayRows, ChildContext child, FeedingFact sched, SchedulePayload sp) {
        List<Row> established = dayRows.stream().filter(r -> !isNewFor(r, child)).toList();
        List<Row> newRows = dayRows.stream().filter(r -> isNewFor(r, child)).toList();

        List<DayVariant> day = new ArrayList<>();
        if (established.isEmpty()) {
            for (Row x : dayRows) {
                String firstNote = x.notes().stream().filter(n -> n.toLowerCase().contains("первого")).findFirst()
                        .map(n -> "Таблица 5.1: «" + n + "» — этот день применим, если ребёнок уже получает другой прикорм")
                        .orElse(null);
                day.add(builder.build(ctx, "Новый продукт: " + x.label(), x.label(), firstNote, List.of(x), x, true));
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
                day.add(builder.build(ctx, "Новый продукт: " + x.label(), x.label(), note, dishes, x, false));
            }
        } else {
            Row fish = sched == null ? null : findRow(established, sched.getSubject());
            Row meat = sched == null || sp == null ? null : findRow(established, sp.replaces());
            if (fish != null && meat != null) {
                final Row f = fish;
                final Row m = meat;
                day.add(builder.build(ctx, "День без новых продуктов, с мясным блюдом", null, null,
                        established.stream().filter(r -> r != f).toList(), null, false));
                day.add(builder.build(ctx, "День без новых продуктов, с рыбным блюдом", null, "Программа: " + sched.getSentence(),
                        established.stream().filter(r -> r != m).toList(), null, false));
            } else {
                day.add(builder.build(ctx, "День без новых продуктов", null, null, established, null, false));
            }
        }
        return day;
    }
}
