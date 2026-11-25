package org.example.tips_for_parents.service.pdf.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Операции над текстом, извлечённым из PDF: склейка строк, выбор предложений вокруг фразы. */
public final class PdfTextUtils {
    private PdfTextUtils() {
    }

    private static final Pattern STOP = Pattern.compile("[.!?](?=\\s|$)");

    /** Склеивает строки PDF в один абзац: убирает дефисные переносы, мягкие дефисы и лишние пробелы. */
    public static String clean(String raw) {
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

    /** n предложений, начиная с anchor; null, если anchor не найден или предложений меньше n. */
    public static String takeSentences(String text, String anchor, int n) {
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

    /** Предложение целиком, в котором встречается phrase; null, если phrase не найдена. */
    public static String sentenceAround(String text, String phrase) {
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

    /** Текст от anchor до endPhrase включительно с многоточием; null, если якоря нет. */
    public static String truncatedAt(String text, String anchor, String endPhrase) {
        int i = text.indexOf(anchor);
        if (i < 0) return null;
        int j = text.indexOf(endPhrase, i);
        if (j < 0) return null;
        return text.substring(i, j + endPhrase.length()).strip() + "…";
    }

    public static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
