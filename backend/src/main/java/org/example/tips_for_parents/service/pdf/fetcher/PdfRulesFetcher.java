package org.example.tips_for_parents.service.pdf.fetcher;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.example.tips_for_parents.model.pdf.RuleExtractionPattern;
import org.example.tips_for_parents.model.pdf.FeedingRecommendationRule;
import org.example.tips_for_parents.service.pdf.util.TextNormalizationUtils;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfRulesFetcher {

    private static final String MARKER = "следует соблюдать следующие правила:";
    private static final String END_MARKER = "Примерная схема назначения";
    private static final Pattern BULLET = Pattern.compile("[•●▪\\uF0B7]");
    private static final Pattern SENTENCE_END = Pattern.compile("[;.](?=\\s|$)");
    private static final Pattern STOP = Pattern.compile("[.!?](?=\\s|$)");
    private static final Pattern MONTHS =
            Pattern.compile("(?iu)в возрасте\\s+(\\d+)(?:\\s*[–-]\\s*(\\d+))?\\s*мес");
    private static final Pattern SCHEDULE = Pattern.compile(
            "(?iu)а\\s+с\\s+(\\d+)\\s+мес\\s*[—–-]\\s*пюре\\s+из\\s+(\\p{L}+),\\s*которое\\s+следует\\s+давать\\s+"
                    + "(\\d+)\\s+раза?/нед\\s+вместо\\s+(\\p{L}+)\\s+блюда");

    private static final List<RuleExtractionPattern> RULE_EXTRACTION_PATTERNS = List.of(
            new RuleExtractionPattern("RULE", "Важно вводить прикорм", 2, null),
            new RuleExtractionPattern("RULE", "Поэтому детям, получающим прикорм", 1, null),
            new RuleExtractionPattern("RULE", "Вначале овощное пюре", 2, "овощ"),
            new RuleExtractionPattern("RULE", "Прикорм на основе злаков", 1, "каш"),
            new RuleExtractionPattern("RESTRICTION", "Неадаптированные кисломолочные напитки", 1, null),
            new RuleExtractionPattern("RESTRICTION", "С 8 мес жизни в рацион вводится детский творог", 1, null),
            new RuleExtractionPattern("RESTRICTION", "Цельное коровье молоко не может использоваться", 1, null));

    public List<FeedingRecommendationRule> extract(byte[] pdf, int tablePage) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            StringBuilder raw = new StringBuilder();
            int first = Math.max(1, tablePage - 1);
            int last = Math.min(doc.getNumberOfPages(), tablePage + 2);
            for (int p = first; p <= last; p++) raw.append('\n').append(twoColumnText(doc, p));
            String text = clean(raw.toString());

            List<FeedingRecommendationRule> feedingRecommendationRules = new ArrayList<>();

            int m = text.indexOf(MARKER);
            if (m < 0) throw new IllegalStateException("Не найден список правил («" + MARKER + "»)");
            int from = m + MARKER.length();
            int to = text.indexOf(END_MARKER, from);
            String window = text.substring(from, to > 0 ? to : Math.min(text.length(), from + 3000));
            for (String seg : BULLET.split(window)) {
                seg = seg.strip();
                if (seg.isEmpty()) continue;
                Matcher end = SENTENCE_END.matcher(seg);
                String bullet = end.find() ? seg.substring(0, end.end()) : seg;   // отсекаем шум таблицы после пункта
                feedingRecommendationRules.add(withMonths("RULE", capitalize(bullet.replaceFirst(";$", ".")), null));
            }

            // 2) отдельные утверждения
            for (RuleExtractionPattern ruleExtractionPattern : RULE_EXTRACTION_PATTERNS) {
                String s = takeSentences(text, ruleExtractionPattern.anchor(), ruleExtractionPattern.sentences());
                if (s != null)
                    feedingRecommendationRules.add(withMonths(ruleExtractionPattern.type(), capitalize(s.replaceFirst("^Поэтому\\s+", "")), ruleExtractionPattern.subject()));
            }

            // 3) «С 7 мес желток, а с 8 мес пюре из рыбы 2 раза/нед вместо мясного блюда»
            String sched = sentenceAround(text, "вместо мясного блюда");
            if (sched != null) {
                Matcher sm = SCHEDULE.matcher(sched);
                if (sm.find()) {
                    String payload = "{\"timesPerWeek\":" + sm.group(3) + ",\"replaces\":\""
                            + TextNormalizationUtils.of(sm.group(4)) + "\"}";
                    feedingRecommendationRules.add(new FeedingRecommendationRule("SCHEDULE", sched, TextNormalizationUtils.of(sm.group(2)),
                            Integer.parseInt(sm.group(1)), null, payload));
                }
            }

            String optional = truncatedAt(text, "Соки, учитывая их незначительную пищевую ценность", "введения всех");
            if (optional != null) feedingRecommendationRules.add(new FeedingRecommendationRule("OPTIONAL", optional, "сок", null, null, null));

            if (feedingRecommendationRules.isEmpty()) throw new IllegalStateException("Правила не извлечены");
            return feedingRecommendationRules;
        }
    }

    public String pageText(byte[] pdf, int pageNumber) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return clean(twoColumnText(doc, pageNumber));
        }
    }

    private static FeedingRecommendationRule withMonths(String type, String text, String subject) {
        Matcher m = MONTHS.matcher(text);
        if (!m.find()) return new FeedingRecommendationRule(type, text, subject, null, null, null);
        return new FeedingRecommendationRule(type, text, subject, Integer.parseInt(m.group(1)),
                m.group(2) != null ? Integer.parseInt(m.group(2)) : null, null);
    }

    private static String twoColumnText(PDDocument doc, int pageNumber) throws IOException {
        PDPage page = doc.getPage(pageNumber - 1);
        float w = page.getMediaBox().getWidth();
        float h = page.getMediaBox().getHeight();
        PDFTextStripperByArea s = new PDFTextStripperByArea();
        s.setSortByPosition(true);
        s.addRegion("left", new Rectangle2D.Float(0, 0, w / 2, h));
        s.addRegion("right", new Rectangle2D.Float(w / 2, 0, w / 2, h));
        s.extractRegions(page);
        return s.getTextForRegion("left") + "\n" + s.getTextForRegion("right");
    }

    static String clean(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\\R")) {
            String l = line.strip().replaceFirst("^-(\\s+|$)", "");
            if (!l.isBlank()) sb.append(l).append(' ');
        }
        return sb.toString()
                .replace("\u00AD", "")
                .replaceAll("(?<=\\p{L})-\\s+(?=\\p{Ll})", "")
                .replaceAll("\\s+", " ")
                .strip();
    }

    static String takeSentences(String text, String anchor, int n) {
        int i = text.indexOf(anchor);
        if (i < 0) return null;
        Matcher m = STOP.matcher(text);
        m.region(i, text.length());
        int end = i;
        for (int k = 0; k < n; k++) {
            if (!m.find()) return null;
            end = m.end();
        }
        return text.substring(i, end).strip();
    }

    static String sentenceAround(String text, String phrase) {
        int i = text.indexOf(phrase);
        if (i < 0) return null;
        int start = 0;
        Matcher b = STOP.matcher(text);
        b.region(0, i);
        while (b.find()) start = b.end();
        Matcher e = STOP.matcher(text);
        e.region(i, text.length());
        if (!e.find()) return null;
        return text.substring(start, e.end()).strip();
    }

    static String truncatedAt(String text, String anchor, String endPhrase) {
        int i = text.indexOf(anchor);
        if (i < 0) return null;
        int j = text.indexOf(endPhrase, i);
        if (j < 0) return null;
        return text.substring(i, j + endPhrase.length()).strip() + "…";
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
