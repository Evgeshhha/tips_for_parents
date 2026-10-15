package org.example.tips_for_parents.service.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.entity.SourceBlock;
import org.example.tips_for_parents.model.entity.SourceDocument;
import org.example.tips_for_parents.model.enums.ReviewStatus;
import org.example.tips_for_parents.model.pdf.ExtractedBlockDraft;
import org.example.tips_for_parents.model.pdf.PdfExtractionResult;
import org.example.tips_for_parents.model.report.SourceImportReport;
import org.example.tips_for_parents.model.report.RefreshReport;
import org.example.tips_for_parents.repository.FeedingFactRepository;
import org.example.tips_for_parents.repository.SourceBlockRepository;
import org.example.tips_for_parents.repository.SourceDocumentRepository;
import org.example.tips_for_parents.service.pdf.extractor.PdfDataExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SourceImportService {
    private final Downloader downloader;
    private final PdfDataExtractor pdfDataExtractor;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceBlockRepository sourceBlockRepository;
    private final FeedingFactRepository feedingFactRepository;
    private final AutoReviewService autoReviewService;
    private final TransactionTemplate tx;
    @Value("${sources.program-pdf-url}")
    private String pdfUrl;

    private volatile RefreshReport last;

    public RefreshReport refresh(String url) {
        String target = url != null && !url.isBlank() ? url : pdfUrl;
        SourceImportReport report;
        try {
            report = ingestPdf(target);
        } catch (Exception e) {
            log.error("Ошибка загрузки источника {}", target, e);
            report = new SourceImportReport("pdf", "ERROR", null, 0, List.of(String.valueOf(e.getMessage())));
        }
        last = new RefreshReport(List.of(report), autoReviewService.run());
        return last;
    }

    public RefreshReport lastRefresh() {
        return last;
    }

    private SourceImportReport ingestPdf(String url) throws Exception {
        byte[] pdf = downloader.get(url);
        String hash = Hashes.sha256(pdf);
        var existing = sourceDocumentRepository.findByUrlAndContentHash(url, hash);
        if (existing.isPresent())
            return new SourceImportReport("pdf", "UNCHANGED", existing.get().getId(), 0, List.of());

        PdfExtractionResult ex = pdfDataExtractor.extract(pdf);        // вне транзакции
        return tx.execute(st -> persist(url, hash, ex));
    }

    private SourceImportReport persist(String url, String hash, PdfExtractionResult ex) {
        String raw = ex.blocks().stream().map(ExtractedBlockDraft::text).collect(Collectors.joining("\n\n"));

        SourceDocument sourceDocument = new SourceDocument();
        sourceDocument.setUrl(url);
        sourceDocument.setContentHash(hash);
        sourceDocument.setRawText(raw);
        sourceDocumentRepository.save(sourceDocument);

        Map<Integer, SourceBlock> byPos = new HashMap<>();
        for (var b : ex.blocks()){
            SourceBlock sourceBlock = new SourceBlock();
            sourceBlock.setDocument(sourceDocument);
            sourceBlock.setPos(b.position());
            sourceBlock.setHeading(b.heading());
            sourceBlock.setText(b.text());

            byPos.put(b.position(), sourceBlockRepository.save(sourceBlock));
        }
        List<FeedingFact> created = new ArrayList<>();
        for (var d : ex.facts()) {
            FeedingFact f = new FeedingFact();
            f.setBlock(byPos.get(d.block()));
            f.setFactType(d.type());
            f.setSubject(d.subject());
            f.setValueText(d.valueText());
            f.setValueMin(d.valueMin());
            f.setValueMax(d.valueMax());
            f.setSentence(d.sentence());
            f.setPayload(d.payload());
            f.setConfidence(d.confidence());
            created.add(f);
        }
        carryOverDecisions(url, sourceDocument.getId(), created);
        feedingFactRepository.saveAll(created);
        return new SourceImportReport("pdf", "UPDATED", sourceDocument.getId(), created.size(), ex.notes());
    }

    private void carryOverDecisions(String url, Long newDocId, List<FeedingFact> created) {
        List<FeedingFact> old = feedingFactRepository.findActiveOfOtherVersions(url, newDocId, ReviewStatus.SUPERSEDED);
        Map<String, FeedingFact> manual = new HashMap<>();
        for (FeedingFact o : old)
            if ("MANUAL".equals(o.getReviewedBy()) && o.getReviewStatus() != ReviewStatus.PENDING)
                manual.put(key(o), o);

        for (FeedingFact n : created) {
            FeedingFact o = manual.get(key(n));
            if (o == null) continue;
            n.setReviewStatus(o.getReviewStatus());
            n.setReviewedBy("MANUAL");
            n.setReviewNote("решение перенесено из предыдущей версии источника (факт #" + o.getId() + ")");
        }
        old.forEach(o -> o.setReviewStatus(ReviewStatus.SUPERSEDED));
    }

    private static String key(FeedingFact f) {
        return f.getFactType() + "|" + f.getSubject() + "|" + f.getValueText() + "|" + f.getSentence();
    }

    public FeedingFact review(long id, ReviewStatus status, String note) {
        if (status == ReviewStatus.SUPERSEDED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPERSEDED выставляется системой");
        return tx.execute(st -> {
            FeedingFact f = feedingFactRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Факт не найден: " + id));
            f.setReviewStatus(status);
            f.setReviewedBy("MANUAL");
            f.setReviewNote(note);
            return f;
        });
    }
}
