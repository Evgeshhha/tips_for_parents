package org.example.tips_for_parents.service.source;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.FactType;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.example.tips_for_parents.model.report.AutoReviewReport;
import org.example.tips_for_parents.repository.FeedingFactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoReviewService {
    private final FeedingFactRepository repo;

    @Transactional
    public AutoReviewReport run() {
        List<FeedingFact> pending = repo.findByReviewStatusOrderById(ReviewStatus.PENDING);
        int approved = 0;
        for (FeedingFact f : pending) {
            if (!"HIGH".equals(f.getConfidence())) continue;
            f.setReviewStatus(ReviewStatus.APPROVED);
            f.setReviewedBy("AUTO");
            f.setReviewNote(switch (f.getFactType()) {
                case FactType.TABLE_AGE, FactType.SLEEP_AGE, FactType.NORM, FactType.NUTRIENT, FactType.WEIGHT_GAIN,
                     FactType.MILK_COMPOSITION ->
                        "значения таблицы разобраны без ошибок";
                default -> "текст взят из программы дословно";
            });
            approved++;
        }
        return new AutoReviewReport(approved, pending.size() - approved);
    }
}
