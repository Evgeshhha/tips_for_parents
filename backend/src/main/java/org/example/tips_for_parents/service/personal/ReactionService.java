package org.example.tips_for_parents.service.personal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tips_for_parents.model.entity.Child;
import org.example.tips_for_parents.model.entity.ReactionReport;
import org.example.tips_for_parents.model.enums.ProductStatus;
import org.example.tips_for_parents.model.enums.ReactionAction;
import org.example.tips_for_parents.model.enums.ReactionStatus;
import org.example.tips_for_parents.model.enums.Urgency;
import org.example.tips_for_parents.model.personal.ReactionAnalysis;
import org.example.tips_for_parents.model.personal.ReactionContext;
import org.example.tips_for_parents.model.personal.ReactionView;
import org.example.tips_for_parents.repository.ChildProductRepository;
import org.example.tips_for_parents.repository.ReactionReportRepository;
import org.example.tips_for_parents.service.card.MonthCardService;
import org.example.tips_for_parents.service.llm.AnthropicReactionAnalyzer;
import org.example.tips_for_parents.service.llm.KeywordReactionAnalyzer;
import org.example.tips_for_parents.service.llm.RedFlagRules;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Поток: текст родителя -> разбор (ИИ или словарь) -> страховка правилами -> предложение (PROPOSED)
 * -> решение родителя (confirm/reject). Статус продукта и план меняются только после confirm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionService {

    private static final int MAX_TEXT = 2000;

    private final ChildService childService;
    private final PlanService plans;
    private final MonthCardService cards;
    private final ChildProductRepository products;
    private final ReactionReportRepository reports;
    private final ObjectProvider<AnthropicReactionAnalyzer> llm;
    private final KeywordReactionAnalyzer fallback;
    private final ObjectMapper om;

    @Transactional
    public ReactionView submit(UUID childId, String text, String productCode) {
        Child child = childService.get(childId);
        String clean = text.strip();
        if (clean.length() > MAX_TEXT) clean = clean.substring(0, MAX_TEXT);

        Set<String> allowed = cards.productCatalog().keySet();
        String hint = null;
        if (productCode != null && !productCode.isBlank()) {
            hint = MonthCardService.productKey(productCode);
            if (!allowed.contains(hint))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный продукт: " + productCode);
        }
        Map<String, ProductStatus> statuses = new HashMap<>();
        products.findByChildId(childId).forEach(p -> statuses.put(p.getProductCode(), p.getStatus()));

        ReactionContext ctx = new ReactionContext(Ages.months(child.getBirthDate(), LocalDate.now()), allowed, statuses, hint);
        ReactionAnalysis analysis = analyze(ctx, clean);

        Urgency byRules = RedFlagRules.evaluate(clean);                 // срочность = max(ИИ, правила)
        if (byRules.ordinal() > analysis.urgency().ordinal()) analysis = analysis.withUrgency(byRules);

        ReactionReport report = reports.save(new ReactionReport(child, analysis.productCode(), clean,
                om.valueToTree(analysis), analysis.urgency(), analysis.suggestedAction(), analysis.llmUsed()));
        return ReactionView.of(report, analysis, RedFlagRules.notice(analysis.urgency()));
    }

    private ReactionAnalysis analyze(ReactionContext ctx, String text) {
        AnthropicReactionAnalyzer analyzer = llm.getIfAvailable();
        if (analyzer != null) {
            try {
                return analyzer.analyze(ctx, text);
            } catch (Exception e) {
                log.warn("Разбор через LLM не удался, используется словарный разбор: {}", e.toString());
            }
        }
        return fallback.analyze(ctx, text);
    }

    /** Родитель подтверждает действие; override == null -> берётся предложенное. */
    @Transactional
    public ReactionView confirm(UUID childId, UUID reportId, ReactionAction override) {
        ReactionReport report = proposed(childId, reportId);
        ReactionAction action = override != null ? override : report.getSuggestedAction();

        if (action == ReactionAction.PAUSE_PRODUCT) {
            if (report.getProductCode() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для этой реакции не определён продукт");
            childService.setProductStatus(childId, report.getProductCode(), ProductStatus.PAUSED, null,
                    "reaction: " + report.getProductCode());
        } else {
            plans.regenerate(childId, "reaction reviewed, no change");
        }
        report.resolve(ReactionStatus.CONFIRMED);
        return view(report);
    }

    @Transactional
    public ReactionView reject(UUID childId, UUID reportId) {
        ReactionReport report = proposed(childId, reportId);
        report.resolve(ReactionStatus.REJECTED);
        return view(report);
    }

    @Transactional(readOnly = true)
    public List<ReactionView> list(UUID childId) {
        childService.get(childId);
        return reports.findByChildIdOrderByCreatedAtDesc(childId).stream().map(this::view).toList();
    }

    private ReactionReport proposed(UUID childId, UUID reportId) {
        ReactionReport r = reports.findByIdAndChildId(reportId, childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение о реакции не найдено"));
        if (r.getStatus() != ReactionStatus.PROPOSED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Решение по этой реакции уже принято");
        return r;
    }

    private ReactionView view(ReactionReport r) {
        try {
            ReactionAnalysis a = om.treeToValue(r.getAnalysis(), ReactionAnalysis.class);
            return ReactionView.of(r, a, RedFlagRules.notice(r.getUrgency()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Повреждён JSON разбора реакции " + r.getId(), e);
        }
    }
}
