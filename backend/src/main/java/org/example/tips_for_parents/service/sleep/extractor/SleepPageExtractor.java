package org.example.tips_for_parents.service.sleep.extractor;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.enums.FactType;
import org.example.tips_for_parents.model.pdf.ExtractedBlockDraft;
import org.example.tips_for_parents.model.pdf.ExtractedFactDraft;
import org.example.tips_for_parents.model.pdf.PdfExtractionResult;
import org.example.tips_for_parents.model.sleep.SleepRow;
import org.example.tips_for_parents.service.common.JsonSupport;
import org.example.tips_for_parents.service.pdf.util.TableText;
import org.example.tips_for_parents.service.sleep.parser.SleepTableParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Извлекает данные о сне со страницы НМИЦ здоровья детей «Режим дня для ребенка до 1 года».
 * SLEEP_AGE — строки таблицы (числа в payload), SLEEP_TEXT — абзацы страницы о сне, дословно.
 * Возвращает тот же PdfExtractionResult, что и PDF-экстрактор, чтобы переиспользовать общий импорт.
 */
@Component
@RequiredArgsConstructor
public class SleepPageExtractor {
    private static final String HIGH = "HIGH";
    private static final int MIN_PARAGRAPH = 40;

    private static final Pattern SLEEP_WORD = Pattern.compile(
            "(?iu)(?<![\\p{L}])(сон|сна|сну|сном|сне|спит|засып\\p{L}*)(?![\\p{L}])");

    private final JsonSupport json;

    public PdfExtractionResult extract(byte[] html, String url) throws IOException {
        Document doc = Jsoup.parse(new ByteArrayInputStream(html), null, url);
        List<ExtractedBlockDraft> blocks = new ArrayList<>();
        List<ExtractedFactDraft> facts = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        List<List<String>> tableRows = findSleepTable(doc)
                .orElseThrow(() -> new IllegalStateException(
                        "Таблица «Режим дневного сна / Ночной сон / Режим бодрствования» не найдена в HTML страницы"));
        var parsed = SleepTableParser.parse(tableRows);
        if (parsed.rows().isEmpty())
            throw new IllegalStateException("Таблица сна найдена, но ни одна строка не разобрана: " + parsed.skipped());

        blocks.add(new ExtractedBlockDraft(0, "Таблица режима сна и бодрствования (НМИЦ здоровья детей)",
                TableText.of(tableRows)));
        for (SleepRow r : parsed.rows()) {
            String sentence = r.label() + ": дневной сон — " + r.data().napsText() + "; ночной сон — " + r.data().nightText()
                    + "; бодрствование — " + r.data().wakeText();
            facts.add(new ExtractedFactDraft(0, FactType.SLEEP_AGE, null, r.label(), r.monthFrom(), r.monthTo(), sentence,
                    json.toJson(r.data()), HIGH));
        }
        notes.add("таблица сна: строк " + parsed.rows().size()
                + (parsed.skipped().isEmpty() ? "" : ", пропущено: " + parsed.skipped()));

        List<String> paragraphs = sleepParagraphs(doc);
        if (paragraphs.isEmpty()) {
            notes.add("абзацы о сне не найдены");
        } else {
            int b = blocks.size();
            blocks.add(new ExtractedBlockDraft(b, "Текст страницы о режиме сна (НМИЦ здоровья детей)", String.join("\n", paragraphs)));
            for (String p : paragraphs)
                facts.add(new ExtractedFactDraft(b, FactType.SLEEP_TEXT, null, null, null, null, p, null, HIGH));
            notes.add("абзацев о сне: " + paragraphs.size());
        }
        return new PdfExtractionResult(blocks, facts, notes);
    }

    private static Optional<List<List<String>>> findSleepTable(Document doc) {
        for (Element table : doc.select("table")) {
            List<List<String>> rows = new ArrayList<>();
            for (Element tr : table.select("tr")) {
                List<String> cells = new ArrayList<>();
                for (Element c : tr.select("th, td")) cells.add(SleepTableParser.clean(c.text()));
                rows.add(cells);
            }
            if (rows.stream().anyMatch(SleepTableParser::isHeader)) return Optional.of(rows);
        }
        return Optional.empty();
    }

    /** Абзацы вне таблиц, где упоминается сон; усечённые копии («…») отбрасываются, если на странице есть полный абзац. */
    static List<String> sleepParagraphs(Document doc) {
        Element root = doc.selectFirst("article");
        if (root == null) root = doc.selectFirst("main");
        if (root == null) root = doc.body();
        if (root == null) return List.of();

        LinkedHashSet<String> all = new LinkedHashSet<>();
        for (Element p : root.select("p")) {
            if (p.closest("table") != null) continue;
            String t = SleepTableParser.clean(p.text());
            if (t.length() >= MIN_PARAGRAPH && SLEEP_WORD.matcher(t).find()) all.add(t);
        }
        List<String> out = new ArrayList<>();
        for (String t : all) {
            String stem = t.replaceAll("(…|\\.\\.\\.)$", "").trim();
            boolean truncatedCopy = all.stream().anyMatch(u -> !u.equals(t) && u.length() > stem.length() && u.startsWith(stem));
            if (!truncatedCopy) out.add(t);
        }
        return out;
    }

}
