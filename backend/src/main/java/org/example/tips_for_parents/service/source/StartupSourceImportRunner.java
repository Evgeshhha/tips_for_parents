package org.example.tips_for_parents.service.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSourceImportRunner {
    private final SourceImportService ingest;
    @Value("${ingest.on-startup:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!enabled) return;
        CompletableFuture.runAsync(() -> {
            log.info("Загрузка PDF при старте…");
            var report = ingest.refresh(null);
            report.sources().forEach(r -> {
                log.info("Источник {}: {} (документ {}, фактов {})", r.source(), r.status(), r.documentId(), r.facts());
                r.notes().forEach(n -> log.info("  • {}", n));
            });
            log.info("Автоподтверждение: подтверждено {}, на ревью {}",
                    report.autoReview().approved(), report.autoReview().stillPending());
        });
    }
}
