package org.example.tips_for_parents.service.pdf.parser;

import org.example.tips_for_parents.model.pdf.FeedingAgeCell;
import org.example.tips_for_parents.model.pdf.ParsedFeedingTable;
import org.example.tips_for_parents.model.pdf.FeedingTableRow;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FeedingTableParser {
    private static final Pattern AGE = Pattern.compile("^(\\d+)(?:\\s*[–‑-]\\s*(\\d+))?$");
    private static final Pattern STARS = Pattern.compile("\\*+");
    private static final Pattern UNIT = Pattern.compile("\\(([^)]*)\\)");

    public ParsedFeedingTable parse(List<List<String>> table, String noteText) {
        Map<String, String> footnotes = parseFootnotes(noteText);

        List<int[]> ages = new ArrayList<>();
        List<String> ageLabels = new ArrayList<>();
        int headerIdx = -1;
        for (int i = 0; i < table.size() && headerIdx < 0; i++) {
            List<String> r = table.get(i);
            List<int[]> tmp = new ArrayList<>();
            List<String> tmpLabels = new ArrayList<>();
            for (int c = 1; c < r.size(); c++) {
                Matcher m = AGE.matcher(NumberParsingUtils.clean(r.get(c)));
                if (!m.matches()) { tmp.clear(); break; }
                int lo = Integer.parseInt(m.group(1));
                tmp.add(new int[]{lo, m.group(2) != null ? Integer.parseInt(m.group(2)) : lo});
                tmpLabels.add(NumberParsingUtils.clean(r.get(c)));
            }
            if (tmp.size() >= 2) { headerIdx = i; ages = tmp; ageLabels = tmpLabels; }
        }
        if (headerIdx < 0) throw new IllegalStateException("Не найдена строка с возрастами (мес.) в таблице 5.1");

        String unit = null;
        for (int i = 0; i < headerIdx && unit == null; i++) {
            Matcher m = UNIT.matcher(NumberParsingUtils.clean(table.get(i).get(0)));
            if (m.find()) unit = m.group(1).strip();
        }

        List<FeedingTableRow> feedingTableRows = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int i = headerIdx + 1; i < table.size(); i++) {
            List<String> r = table.get(i);
            String rawLabel = r.get(0);
            String label = NumberParsingUtils.clean(rawLabel);
            if (label.isEmpty()) continue;
            if (r.size() < ages.size() + 1) { skipped.add(label + " (неполная строка)"); continue; }

            List<FeedingAgeCell> feedingAgeCells = new ArrayList<>();
            for (int c = 0; c < ages.size(); c++) {
                String v = NumberParsingUtils.clean(r.get(c + 1));
                if (v.isEmpty() || v.matches("[-–—]+")) continue;      // в этом возрасте не вводится
                feedingAgeCells.add(new FeedingAgeCell(ageLabels.get(c), ages.get(c)[0], ages.get(c)[1], v));
            }
            if (feedingAgeCells.isEmpty()) { skipped.add(label + " (нет значений)"); continue; }

            List<String> notes = new ArrayList<>();
            Matcher sm = STARS.matcher(rawLabel);
            while (sm.find()) {
                String fn = footnotes.get(sm.group());
                if (fn != null) notes.add(fn);
            }
            feedingTableRows.add(new FeedingTableRow(label, feedingAgeCells.get(0).lo(), feedingAgeCells, notes));
        }
        return new ParsedFeedingTable(feedingTableRows, unit, skipped);
    }

    static Map<String, String> parseFootnotes(String note) {
        Map<String, String> out = new LinkedHashMap<>();
        if (note == null) return out;
        String body = note.replaceFirst("^\\s*Примечание\\.?", "");
        for (String chunk : body.split("(?<!\\*)(?=\\*+\\s*[—–-])")) {
            Matcher m = Pattern.compile("^(\\*+)\\s*[—–-]\\s*(.+?)[\\s,;.]*$", Pattern.DOTALL).matcher(chunk.strip());
            if (m.matches()) out.put(m.group(1), NumberParsingUtils.clean(m.group(2)));
        }
        return out;
    }
}
