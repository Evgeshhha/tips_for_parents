package org.example.tips_for_parents.service.card;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.card.MilkInfo;
import org.example.tips_for_parents.model.card.Needs;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.FactType;
import org.example.tips_for_parents.model.payload.MilkPayload;
import org.example.tips_for_parents.model.payload.VolumePayload;
import org.example.tips_for_parents.model.value.Nutrients;
import org.example.tips_for_parents.service.common.JsonSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/** Состав и объём молока/смеси, который вычитается из суточной потребности. */
@Component
@RequiredArgsConstructor
public class MilkCalculator {
    private final JsonSupport json;

    /** null, если состава или объёма нет; причина добавляется в warnings. */
    MilkInfo calculate(int month, String kind, Double milkMl, Needs needs, ApprovedFacts facts, List<String> warnings) {
        FeedingFact comp = facts.of(FactType.MILK_COMPOSITION).stream()
                .filter(f -> kind.equals(f.getSubject())).findFirst().orElse(null);
        MilkPayload mp = comp == null ? null : json.readOrNull(comp.getPayload(), MilkPayload.class);
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
            FeedingFact cap = facts.of(FactType.MILK_VOLUME).stream()
                    .filter(f -> f.getValueMin() != null && f.getValueMin() <= month && (f.getValueMax() == null || month <= f.getValueMax()))
                    .findFirst().orElse(null);
            VolumePayload vp = cap == null ? null : json.readOrNull(cap.getPayload(), VolumePayload.class);
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
}
