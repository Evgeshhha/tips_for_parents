package org.example.tips_for_parents.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Таблица 5.1: строки = продукты/блюда, столбцы = возраст (мес). Без словарей: строка = как в таблице.
 * Чистая функция над List<List<String>> — тестируется без PDF.
 */
@Component
public class FeedingTableParser {

    public record Cell(String age, int lo, int hi, String amount) {}
    public record Row(String label, int minAge, List<Cell> cells, List<String> footnotes) {}
    public record Parsed(List<Row> rows, String unit, List<String> skipped) {}

    private static final Pattern AGE = Pattern.compile("^(\\d+)(?:\\s*[–‑-]\\s*(\\d+))?$");
    private static final Pattern STARS = Pattern.compile("\\*+");
    private static final Pattern UNIT = Pattern.compile("\\(([^)]*)\\)");

    public Parsed parse(List<List<String>> table, String noteText) {
        Map<String, String> footnotes = parseFootnotes(noteText);

        List<int[]> ages = new ArrayList<>();
        List<String> ageLabels = new ArrayList<>();
        int headerIdx = -1;
        for (int i = 0; i < table.size() && headerIdx < 0; i++) {
            List<String> r = table.get(i);
            List<int[]> tmp = new ArrayList<>();
            List<String> tmpLabels = new ArrayList<>();
            for (int c = 1; c < r.size(); c++) {
                Matcher m = AGE.matcher(NumberParser.clean(r.get(c)));
                if (!m.matches()) { tmp.clear(); break; }
                int lo = Integer.parseInt(m.group(1));
                tmp.add(new int[]{lo, m.group(2) != null ? Integer.parseInt(m.group(2)) : lo});
                tmpLabels.add(NumberParser.clean(r.get(c)));
            }
            if (tmp.size() >= 2) { headerIdx = i; ages = tmp; ageLabels = tmpLabels; }
        }
        if (headerIdx < 0) throw new IllegalStateException("Не найдена строка с возрастами (мес.) в таблице 5.1");

        String unit = null;
        for (int i = 0; i < headerIdx && unit == null; i++) {
            Matcher m = UNIT.matcher(NumberParser.clean(table.get(i).get(0)));
            if (m.find()) unit = m.group(1).strip();
        }

        List<Row> rows = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int i = headerIdx + 1; i < table.size(); i++) {
            List<String> r = table.get(i);
            String rawLabel = r.get(0);
            String label = NumberParser.clean(rawLabel);
            if (label.isEmpty()) continue;
            if (r.size() < ages.size() + 1) { skipped.add(label + " (неполная строка)"); continue; }

            List<Cell> cells = new ArrayList<>();
            for (int c = 0; c < ages.size(); c++) {
                String v = NumberParser.clean(r.get(c + 1));
                if (v.isEmpty() || v.matches("[-–—]+")) continue;      // в этом возрасте не вводится
                cells.add(new Cell(ageLabels.get(c), ages.get(c)[0], ages.get(c)[1], v));
            }
            if (cells.isEmpty()) { skipped.add(label + " (нет значений)"); continue; }

            List<String> notes = new ArrayList<>();
            Matcher sm = STARS.matcher(rawLabel);
            while (sm.find()) {
                String fn = footnotes.get(sm.group());
                if (fn != null) notes.add(fn);
            }
            rows.add(new Row(label, cells.get(0).lo(), cells, notes));
        }
        return new Parsed(rows, unit, skipped);
    }

    /** «Примечание. * — ..., ** — ...; *** — ...» -> {"*": "...", "**": "..."} */
    static Map<String, String> parseFootnotes(String note) {
        Map<String, String> out = new LinkedHashMap<>();
        if (note == null) return out;
        String body = note.replaceFirst("^\\s*Примечание\\.?", "");
        for (String chunk : body.split("(?<!\\*)(?=\\*+\\s*[—–-])")) {
            Matcher m = Pattern.compile("^(\\*+)\\s*[—–-]\\s*(.+?)[\\s,;.]*$", Pattern.DOTALL).matcher(chunk.strip());
            if (m.matches()) out.put(m.group(1), NumberParser.clean(m.group(2)));
        }
        return out;
    }
}
