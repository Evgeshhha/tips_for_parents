package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.example.tips_for_parents.model.report.AutoReviewReport;
import org.example.tips_for_parents.model.report.RefreshReport;
import org.example.tips_for_parents.repository.FeedingFactRepository;
import org.example.tips_for_parents.service.source.AutoReviewService;
import org.example.tips_for_parents.service.source.SourceImportService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Админка: загрузка PDF и ревью фактов")
public class AdminController {

    public record FactView(Long id, String type, String subject, String valueText, Integer valueMin, Integer valueMax,
                           String sentence, ReviewStatus status, String confidence, String reviewedBy, String reviewNote) {
        static FactView of(FeedingFact f) {
            return new FactView(f.getId(), f.getFactType(), f.getSubject(), f.getValueText(), f.getValueMin(), f.getValueMax(), f.getSentence(),
                    f.getReviewStatus(), f.getConfidence(), f.getReviewedBy(), f.getReviewNote());
        }
    }

    public record Status(Map<String, Map<String, Long>> factsByTypeAndStatus, RefreshReport lastRefresh) {}

    private final SourceImportService ingest;
    private final AutoReviewService autoReview;
    private final FeedingFactRepository facts;

    @Operation(summary = "Скачать и разобрать PDF программы, прогнать автоподтверждение",
            description = "Если файл не изменился (хеш) — пропускается. При изменении факты прежней версии получают SUPERSEDED, "
                    + "ручные решения ревью переносятся на совпавшие факты. Ошибки — в поле status/notes отчёта.")
    @PostMapping("/refresh")
    public RefreshReport refresh(
            @Parameter(description = "URL PDF; по умолчанию из sources.program-pdf-url") @RequestParam(required = false) String url) {
        return ingest.refresh(url);
    }

    @Operation(summary = "Прогнать автоподтверждение очевидных фактов по всем PENDING")
    @PostMapping("/auto-review")
    public AutoReviewReport autoReview() {
        return autoReview.run();
    }

    @Operation(summary = "Состояние данных: сколько фактов каждого типа в каждом статусе + отчёт последней загрузки",
            description = "Если карточка пустая или без правил/норм — смотрите здесь, что извлеклось и что ждёт ревью.")
    @GetMapping("/status")
    public Status status() {
        Map<String, Map<String, Long>> counts = new TreeMap<>();
        for (Object[] row : facts.countByTypeAndStatus())
            counts.computeIfAbsent((String) row[0], k -> new TreeMap<>()).put(String.valueOf(row[1]), (Long) row[2]);
        return new Status(counts, ingest.lastRefresh());
    }

    @Operation(summary = "Список фактов по статусу ревью")
    @GetMapping("/facts")
    public List<FactView> facts(@RequestParam(defaultValue = "PENDING") ReviewStatus status) {
        return facts.findByReviewStatusOrderById(status).stream().map(FactView::of).toList();
    }

    @Operation(summary = "Ручное решение по факту (перекрывает автоподтверждение)")
    @PostMapping("/facts/{id}/review")
    public FactView review(@PathVariable long id, @RequestParam ReviewStatus status,
                           @RequestParam(required = false) String note) {
        return FactView.of(ingest.review(id, status, note));
    }
}
