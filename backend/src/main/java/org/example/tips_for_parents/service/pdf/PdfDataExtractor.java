package org.example.tips_for_parents.service.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Из PDF программы делает «черновики» блоков и фактов (без БД). Обязательна только таблица 5.1;
 * остальное (правила, нормы, пищевая ценность, прибавки массы) — по возможности, сбои попадают в notes.
 */
@Component
public class PdfDataExtractor {

    public record BlockDraft(int position, String heading, String text) {}

    public record FactDraft(int block, String type, String subject, String valueText, Integer valueMin,
                            Integer valueMax, String sentence, String payload, String confidence) {}

    public record Extraction(List<BlockDraft> blocks, List<FactDraft> facts, List<String> notes) {}

    private static final String HIGH = "HIGH";

    private final PdfTableFetcher tables;
    private final FeedingTableParser feedingParser;
    private final PdfRulesFetcher rulesFetcher;
    private final ObjectMapper om;

    public PdfDataExtractor(PdfTableFetcher tables, FeedingTableParser feedingParser, PdfRulesFetcher rulesFetcher,
                            ObjectMapper om) {
        this.tables = tables;
        this.feedingParser = feedingParser;
        this.rulesFetcher = rulesFetcher;
        this.om = om;
    }

    public Extraction extract(byte[] pdf) throws IOException {
        List<BlockDraft> blocks = new ArrayList<>();
        List<FactDraft> facts = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // ---- таблица 5.1 (обязательна) ----
        var t51 = tables.feedingScheme(pdf)
                .orElseThrow(() -> new IllegalStateException("Таблица 5.1 «Примерная схема введения продуктов» не найдена"));
        var parsed = feedingParser.parse(t51.rows(), t51.noteText());
        int b0 = addBlock(blocks, "Таблица 5.1 (стр. " + t51.pageNumber() + ")", t51.rows(), t51.noteText());
        for (var row : parsed.rows()) {
            String sentence = row.label() + ": " + row.cells().stream()
                    .map(c -> c.age() + " мес — " + c.amount()).collect(Collectors.joining("; "));
            facts.add(new FactDraft(b0, "TABLE_AGE", null, row.label(), row.minAge(), null, sentence,
                    json(Map.of("unit", parsed.unit() == null ? "" : parsed.unit(),
                            "cells", row.cells(), "footnotes", row.footnotes())), HIGH));
        }
        notes.add("таблица 5.1: строк " + parsed.rows().size()
                + (parsed.skipped().isEmpty() ? "" : ", пропущено: " + parsed.skipped()));

        // ---- правила главы 5 ----
        try {
            var rules = rulesFetcher.extract(pdf, t51.pageNumber());
            int b = addBlock(blocks, "Правила введения прикорма (глава 5)",
                    rules.stream().map(PdfRulesFetcher.Rule::text).collect(Collectors.joining("\n")));
            for (var r : rules)
                facts.add(new FactDraft(b, r.type(), r.subject(), null, r.monthMin(), r.monthMax(), r.text(),
                        r.payload(), HIGH));
            notes.add("правил извлечено: " + rules.size());
        } catch (IOException | RuntimeException e) {
            notes.add("правила не извлечены: " + e.getMessage());
        }

        // ---- приложение 1: нормы на кг массы тела ----
        try {
            var tp = tables.norms(pdf).orElseThrow(() -> new IllegalStateException("таблица не найдена"));
            var norms = NutritionNormsParser.parse(tp.rows());
            int b = addBlock(blocks, "Приложение 1: нормы (стр. " + tp.pageNumber() + ")", tp.rows(), tp.noteText());
            for (var n : norms)
                facts.add(new FactDraft(b, "NORM", null, n.label(), n.monthFrom(), n.monthTo(),
                        n.label() + ": энергия " + NumberParser.fmt(n.energy()) + " ккал/кг, белки " + NumberParser.fmt(n.protein())
                                + " г/кг, жиры " + NumberParser.fmt(n.fat()) + " г/кг, углеводы " + NumberParser.fmt(n.carbs()) + " г/кг в сутки",
                        json(Map.of("energy", n.energy(), "protein", n.protein(), "fat", n.fat(), "carbs", n.carbs())),
                        HIGH));
            notes.add("нормы (приложение 1): " + norms.size());
        } catch (IOException | RuntimeException e) {
            notes.add("нормы (приложение 1) не извлечены: " + e.getMessage());
        }

        // ---- таблица 7.4: пищевая ценность ----
        try {
            var tp = tables.nutrients(pdf).orElseThrow(() -> new IllegalStateException("таблица не найдена"));
            var items = NutrientTableParser.parse(tp.rows());
            int b = addBlock(blocks, "Таблица 7.4 (стр. " + tp.pageNumber() + ")", tp.rows(), tp.noteText());
            for (var n : items)
                facts.add(new FactDraft(b, "NUTRIENT", null, n.name(), null, null,
                        n.name() + " (на " + NumberParser.fmt(n.basisAmount()) + " " + n.basisUnit() + "): белки " + NumberParser.fmt(n.protein())
                                + " г; жиры " + NumberParser.fmt(n.fat()) + " г; углеводы " + NumberParser.fmt(n.carbs()) + " г; "
                                + NumberParser.fmt(n.energy()) + " ккал",
                        json(n), HIGH));
            notes.add("пищевая ценность (таблица 7.4): " + items.size());
        } catch (IOException | RuntimeException e) {
            notes.add("таблица 7.4 не извлечена: " + e.getMessage());
        }

        // ---- таблица 6.1: прибавки массы тела ----
        try {
            var tp = tables.weightGain(pdf).orElseThrow(() -> new IllegalStateException("таблица не найдена"));
            var gains = WeightGainParser.parse(tp.rows());
            int b = addBlock(blocks, "Таблица 6.1 (стр. " + tp.pageNumber() + ")", tp.rows(), tp.noteText());
            for (var g : gains)
                facts.add(new FactDraft(b, "WEIGHT_GAIN", null, g.label(), g.monthFrom(), g.monthTo(),
                        g.label() + ": прибавка массы тела " + NumberParser.fmt(g.gramsPerDay()) + " г/день",
                        json(Map.of("gramsPerDay", g.gramsPerDay())), HIGH));
            notes.add("прибавки массы (таблица 6.1): " + gains.size());
        } catch (IOException | RuntimeException e) {
            notes.add("таблица 6.1 не извлечена: " + e.getMessage());
        }

        return new Extraction(blocks, facts, notes);
    }

    private int addBlock(List<BlockDraft> blocks, String heading, List<List<String>> rows, String note) {
        String text = rows.stream().map(r -> String.join(" | ", r).replaceAll("\\s+", " "))
                .collect(Collectors.joining("\n")) + (note == null || note.isBlank() ? "" : "\n" + note);
        return addBlock(blocks, heading, text);
    }

    private int addBlock(List<BlockDraft> blocks, String heading, String text) {
        blocks.add(new BlockDraft(blocks.size(), heading, text));
        return blocks.size() - 1;
    }

    private String json(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
