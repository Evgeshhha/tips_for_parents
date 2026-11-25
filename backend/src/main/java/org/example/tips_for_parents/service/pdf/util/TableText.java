package org.example.tips_for_parents.service.pdf.util;

import java.util.List;
import java.util.stream.Collectors;

/** Текстовое представление таблицы для блока источника: ячейки через « | », строки через перевод строки. */
public final class TableText {
    private TableText() {
    }

    public static String of(List<List<String>> rows) {
        return rows.stream().map(r -> String.join(" | ", r).replaceAll("\\s+", " ")).collect(Collectors.joining("\n"));
    }
}
