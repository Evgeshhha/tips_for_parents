package org.example.tips_for_parents.service.pdf.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.pdf.*;
import org.example.tips_for_parents.service.pdf.fetcher.PdfRulesFetcher;
import org.example.tips_for_parents.service.pdf.fetcher.PdfTableFetcher;
import org.example.tips_for_parents.service.pdf.parser.FeedingTableParser;
import org.example.tips_for_parents.service.pdf.parser.NutrientTableParser;
import org.example.tips_for_parents.service.pdf.parser.WeightGainParser;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.tips_for_parents.service.pdf.parser.NutritionNormsParser.parse;

@RequiredArgsConstructor
@Component
public class PdfDataExtractor {
    private static final String HIGH = "HIGH";

    private final PdfTableFetcher tables;
    private final FeedingTableParser feedingParser;
    private final PdfRulesFetcher rulesFetcher;
    private final PdfMilkExtractor milkExtractor;
    private final ObjectMapper om;

    public PdfExtractionResult extract(byte[] pdf) throws IOException {
        List<ExtractedBlockDraft> blocks = new ArrayList<>();
        List<ExtractedFactDraft> facts = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        var t51 = tables.feedingScheme(pdf)
                .orElseThrow(() -> new IllegalStateException("Таблица 5.1 «Примерная схема введения продуктов» не найдена"));
        var parsed = feedingParser.parse(t51.rows(), t51.noteText());
        int b0 = addBlock(blocks, "Таблица 5.1 (стр. " + t51.pageNumber() + ")", t51.rows(), t51.noteText());
        for (var row : parsed.feedingTableRows()) {
            String sentence = row.label() + ": " + row.feedingAgeCells().stream()
                    .map(c -> c.age() + " мес — " + c.amount()).collect(Collectors.joining("; "));
            facts.add(new ExtractedFactDraft(b0, "TABLE_AGE", null, row.label(), row.minAge(), null, sentence,
                    json(Map.of("unit", parsed.unit() == null ? "" : parsed.unit(),
                            "cells", row.feedingAgeCells(), "footnotes", row.footnotes())), HIGH));
        }
        notes.add("таблица 5.1: строк " + parsed.feedingTableRows().size()
                + (parsed.skipped().isEmpty() ? "" : ", пропущено: " + parsed.skipped()));

        try {
            var rules = rulesFetcher.extract(pdf, t51.pageNumber());
            int b = addBlock(blocks, "Правила введения прикорма (глава 5)",
                    rules.stream().map(FeedingRecommendationRule::text).collect(Collectors.joining("\n")));
            for (var r : rules)
                facts.add(new ExtractedFactDraft(b, r.type(), r.subject(), null, r.monthMin(), r.monthMax(), r.text(),
                        r.payload(), HIGH));
            notes.add("правил извлечено: " + rules.size());
        } catch (IOException | RuntimeException e) {
            notes.add("правила не извлечены: " + e.getMessage());
        }

        try {
            var tp = tables.norms(pdf).orElseThrow(() -> new IllegalStateException("таблица не найдена"));
            var norms = parse(tp.rows());
            int b = addBlock(blocks, "Приложение 1: нормы (стр. " + tp.pageNumber() + ")", tp.rows(), tp.noteText());
            for (var n : norms)
                facts.add(new ExtractedFactDraft(b, "NORM", null, n.label(), n.monthFrom(), n.monthTo(),
                        n.label() + ": энергия " + NumberParsingUtils.fmt(n.energy()) + " ккал/кг, белки " + NumberParsingUtils.fmt(n.protein())
                                + " г/кг, жиры " + NumberParsingUtils.fmt(n.fat()) + " г/кг, углеводы " + NumberParsingUtils.fmt(n.carbs()) + " г/кг в сутки",
                        json(Map.of("energy", n.energy(), "protein", n.protein(), "fat", n.fat(), "carbs", n.carbs())),
                        HIGH));
            notes.add("нормы (приложение 1): " + norms.size());
        } catch (IOException | RuntimeException e) {
            notes.add("нормы (приложение 1) не извлечены: " + e.getMessage());
        }

        try {
            var tp = tables.nutrients(pdf).orElseThrow(() -> new IllegalStateException("таблица не найдена"));
            var items = NutrientTableParser.parse(tp.rows());
            int b = addBlock(blocks, "Таблица 7.4 (стр. " + tp.pageNumber() + ")", tp.rows(), tp.noteText());
            for (var n : items)
                facts.add(new ExtractedFactDraft(b, "NUTRIENT", null, n.name(), null, null,
                        n.name() + " (на " + NumberParsingUtils.fmt(n.basisAmount()) + " " + n.basisUnit() + "): белки " + NumberParsingUtils.fmt(n.protein())
                                + " г; жиры " + NumberParsingUtils.fmt(n.fat()) + " г; углеводы " + NumberParsingUtils.fmt(n.carbs()) + " г; "
                                + NumberParsingUtils.fmt(n.energy()) + " ккал",
                        json(n), HIGH));
            notes.add("пищевая ценность (таблица 7.4): " + items.size());
        } catch (IOException | RuntimeException e) {
            notes.add("таблица 7.4 не извлечена: " + e.getMessage());
        }

        try {
            var tp = tables.weightGain(pdf).orElseThrow(() -> new IllegalStateException("таблица не найдена"));
            var gains = WeightGainParser.parse(tp.rows());
            int b = addBlock(blocks, "Таблица 6.1 (стр. " + tp.pageNumber() + ")", tp.rows(), tp.noteText());
            for (var g : gains)
                facts.add(new ExtractedFactDraft(b, "WEIGHT_GAIN", null, g.label(), g.monthFrom(), g.monthTo(),
                        g.label() + ": прибавка массы тела " + NumberParsingUtils.fmt(g.gramsPerDay()) + " г/день",
                        json(Map.of("gramsPerDay", g.gramsPerDay())), HIGH));
            notes.add("прибавки массы (таблица 6.1): " + gains.size());
        } catch (IOException | RuntimeException e) {
            notes.add("таблица 6.1 не извлечена: " + e.getMessage());
        }

        try {
            var milk = milkExtractor.extract(pdf);
            if (milk.milkCompositions().isEmpty() && milk.milkVolumeLimits().isEmpty()) {
                notes.add("данные о молоке/смеси не найдены");
            } else {
                int b = addBlock(blocks, "Молоко / смесь: состав и объём",
                        Stream.concat(milk.milkCompositions().stream().map(MilkComposition::sentence),
                                        milk.milkVolumeLimits().stream().map(MilkVolumeLimit::sentence).distinct())
                                .collect(Collectors.joining("\n")));
                for (var c : milk.milkCompositions())
                    facts.add(new ExtractedFactDraft(b, "MILK_COMPOSITION", c.kind(), c.label(), null, null,
                            c.label() + ": " + NumberParsingUtils.fmt(c.energy()) + " ккал, белки " + NumberParsingUtils.fmt(c.protein()) + " г, жиры "
                                    + NumberParsingUtils.fmt(c.fat()) + " г, углеводы " + NumberParsingUtils.fmt(c.carbs()) + " г",
                            json(Map.of("energy", c.energy(), "protein", c.protein(), "fat", c.fat(), "carbs", c.carbs())),
                            HIGH));
                for (var v : milk.milkVolumeLimits())
                    facts.add(new ExtractedFactDraft(b, "MILK_VOLUME", null, v.label(), v.monthFrom(), v.monthTo(), v.sentence(),
                            json(Map.of("maxMl", v.maxMl())), HIGH));
                notes.add("молоко/смесь: составов " + milk.milkCompositions().size() + ", границ объёма " + milk.milkVolumeLimits().size());
            }
        } catch (IOException | RuntimeException e) {
            notes.add("данные о молоке/смеси не извлечены: " + e.getMessage());
        }

        return new PdfExtractionResult(blocks, facts, notes);
    }

    private int addBlock(List<ExtractedBlockDraft> blocks, String heading, List<List<String>> rows, String note) {
        String text = rows.stream().map(r -> String.join(" | ", r).replaceAll("\\s+", " "))
                .collect(Collectors.joining("\n")) + (note == null || note.isBlank() ? "" : "\n" + note);
        return addBlock(blocks, heading, text);
    }

    private int addBlock(List<ExtractedBlockDraft> blocks, String heading, String text) {
        blocks.add(new ExtractedBlockDraft(blocks.size(), heading, text));
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
