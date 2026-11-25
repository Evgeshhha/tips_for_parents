package org.example.tips_for_parents.service.card;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.config.CardsProperties;
import org.example.tips_for_parents.model.card.Needs;
import org.example.tips_for_parents.model.card.Weight;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.FactType;
import org.example.tips_for_parents.model.payload.GainPayload;
import org.example.tips_for_parents.model.payload.NormPayload;
import org.example.tips_for_parents.model.value.Nutrients;
import org.example.tips_for_parents.service.common.JsonSupport;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/** Суточная потребность ребёнка по нормам (приложение 1) и массе (запрос или оценка по таблице 6.1). */
@Component
@RequiredArgsConstructor
public class NeedsCalculator {
    private static final double DAYS_IN_MONTH = 30.4;

    private final JsonSupport json;
    private final CardsProperties props;

    /** null, если норм или массы нет; причина добавляется в warnings. */
    Needs calculate(int month, Double weightKg, Double birthWeightKg, ApprovedFacts facts, List<String> warnings) {
        FeedingFact norm = facts.of(FactType.NORM).stream()
                .filter(f -> f.getValueMin() != null && f.getValueMax() != null && f.getValueMin() <= month && month <= f.getValueMax())
                .findFirst().orElse(null);
        NormPayload np = norm == null ? null : json.readOrNull(norm.getPayload(), NormPayload.class);
        if (np == null) {
            warnings.add("Нет утверждённых норм (приложение 1) для этого возраста — потребность не считается");
            return null;
        }
        Weight w = weight(month, weightKg, birthWeightKg, facts.of(FactType.WEIGHT_GAIN));
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
        double birth = birthWeightKg != null ? birthWeightKg : props.birthWeightKg();
        double kg = birth;
        for (int k = 0; k < month; k++) {
            final int kk = k;
            FeedingFact band = gains.stream()
                    .filter(g -> g.getValueMin() != null && g.getValueMax() != null && g.getValueMin() <= kk && kk < g.getValueMax())
                    .findFirst().orElse(null);
            GainPayload gp = band == null ? null : json.readOrNull(band.getPayload(), GainPayload.class);
            if (gp == null) break;
            kg += (gp.gramsPerDay().lo() + gp.gramsPerDay().hi()) / 2 * DAYS_IN_MONTH / 1000;
        }
        return new Weight(kg, "оценка по средним прибавкам таблицы 6.1 при массе при рождении "
                + NumberParsingUtils.fmt(birth) + " кг (масса при рождении — не из программы, можно передать birthWeightKg)");
    }
}
