package org.example.tips_for_parents.service.sleep;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.card.TextFact;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.FactType;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.example.tips_for_parents.model.sleep.SleepBand;
import org.example.tips_for_parents.model.sleep.SleepBandData;
import org.example.tips_for_parents.model.sleep.SleepCard;
import org.example.tips_for_parents.repository.FeedingFactRepository;
import org.example.tips_for_parents.service.common.JsonSupport;
import org.example.tips_for_parents.service.common.MonthValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Общий план сна: только утверждённые факты, разобранные со страницы НМИЦ. Ничего не вычисляется и не достраивается.
 */
@Service
@RequiredArgsConstructor
public class SleepService {
    public static final int MIN_MONTH = 0;
    public static final int MAX_MONTH = 12;

    private static final List<String> NOTES = List.of(
            "Источник — НМИЦ здоровья детей Минздрава России, «Режим дня для ребенка до 1 года»: возрастные диапазоны, а не отдельные месяцы.",
            "Суммарная продолжительность сна за сутки в источнике не указана — не показывается.",
            "Время укладывания и подъёма в источнике не указано — не показывается.");

    private final FeedingFactRepository repo;
    private final JsonSupport json;

    @Transactional(readOnly = true)
    public SleepCard all() {
        return build(null);
    }

    @Transactional(readOnly = true)
    public SleepCard forMonth(int month) {
        MonthValidator.require(month, MIN_MONTH, MAX_MONTH);
        return build(month);
    }

    private SleepCard build(Integer month) {
        List<String> warnings = new ArrayList<>();
        List<FeedingFact> ageFacts = repo.findByFactTypeAndReviewStatusOrderById(FactType.SLEEP_AGE, ReviewStatus.APPROVED);
        List<FeedingFact> textFacts = repo.findByFactTypeAndReviewStatusOrderById(FactType.SLEEP_TEXT, ReviewStatus.APPROVED);

        List<SleepBand> bands = new ArrayList<>();
        for (FeedingFact f : ageFacts) {
            if (f.getValueMin() == null || f.getValueMax() == null || f.getPayload() == null) {
                warnings.add("Факт #" + f.getId() + " неполный и пропущен");
                continue;
            }
            SleepBandData data = json.readOrNull(f.getPayload(), SleepBandData.class);
            if (data == null) {
                warnings.add("Факт #" + f.getId() + " не удалось прочитать и пропущен");
                continue;
            }
            bands.add(new SleepBand(f.getValueText(), f.getValueMin(), f.getValueMax(), data, f.getSentence(), f.getId()));
        }
        bands.sort(Comparator.comparingInt(SleepBand::monthFrom).thenComparingInt(SleepBand::monthTo));

        if (bands.isEmpty())
            warnings.add("Данные о сне не загружены или не утверждены: смотрите GET /api/admin/status");

        List<SleepBand> selected = bands;
        if (month != null) {
            selected = bands.stream().filter(b -> b.monthFrom() <= month && month <= b.monthTo()).toList();
            if (selected.isEmpty() && !bands.isEmpty())
                warnings.add("Для месяца " + month + " в источнике нет данных");
            if (selected.size() > 1)
                warnings.add("Месяц " + month + " — граница двух диапазонов; источник не говорит, к какому его отнести, показаны оба");
        }

        List<TextFact> rhythm = textFacts.stream().map(f -> new TextFact(f.getSentence(), f.getId())).toList();
        String url = ageFacts.isEmpty() || ageFacts.get(0).getBlock() == null ? null
                : ageFacts.get(0).getBlock().getDocument().getUrl();
        return new SleepCard(month, url, selected, rhythm, NOTES, warnings);
    }
}
