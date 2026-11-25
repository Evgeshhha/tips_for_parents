package org.example.tips_for_parents.service.card;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.card.Row;
import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.payload.TablePayload;
import org.example.tips_for_parents.model.pdf.FeedingAgeCell;
import org.example.tips_for_parents.service.common.JsonSupport;
import org.example.tips_for_parents.service.pdf.util.TextNormalizationUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Чтение утверждённых строк таблицы 5.1: строки для месяца и каталог продуктов. */
@Component
@RequiredArgsConstructor
public class FeedingTableReader {
    private static final Pattern ADDON = Pattern.compile("(?iu)^добавляется к\\s+(.+)$");

    private final JsonSupport json;

    List<Row> rows(int month, List<FeedingFact> tableFacts) {
        List<Row> rows = new ArrayList<>();
        for (FeedingFact f : tableFacts) {
            TablePayload pl = json.readOrNull(f.getPayload(), TablePayload.class);
            if (pl == null || pl.cells() == null || pl.cells().isEmpty()) continue;
            FeedingAgeCell cell = pl.cells().stream()
                    .filter(c -> c.lo() <= month && month <= c.hi()).findFirst().orElse(null);
            if (cell == null) continue;                       // в этом возрасте продукт не вводится

            String addonStem = null;
            List<String> notes = new ArrayList<>();
            if (pl.footnotes() != null) {
                for (String fn : pl.footnotes()) {
                    Matcher m = ADDON.matcher(fn);
                    if (m.matches()) addonStem = TextNormalizationUtils.of(TextNormalizationUtils.firstWord(m.group(1)));   // «овощному пюре» -> «овощ»
                    else notes.add(fn);
                }
            }
            rows.add(new Row(f, cell, pl.unit(), cell.lo() == pl.cells().getFirst().lo(), addonStem, notes));
        }
        return rows;
    }

    /** Продукты таблицы 5.1 (без добавок вроде масла): ключ -> название. */
    Map<String, String> catalog(List<FeedingFact> tableFacts) {
        Map<String, String> res = new LinkedHashMap<>();
        for (FeedingFact f : tableFacts) {
            if (f.getValueText() == null) continue;
            TablePayload pl = json.readOrNull(f.getPayload(), TablePayload.class);
            if (pl == null) continue;
            boolean addon = pl.footnotes() != null && pl.footnotes().stream().anyMatch(fn -> ADDON.matcher(fn).matches());
            if (!addon) res.putIfAbsent(ProductKeys.of(f.getValueText()), f.getValueText());
        }
        return res;
    }
}
