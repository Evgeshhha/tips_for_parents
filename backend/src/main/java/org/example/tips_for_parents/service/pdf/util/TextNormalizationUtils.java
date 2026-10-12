package org.example.tips_for_parents.service.pdf.util;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class TextNormalizationUtils {

    public static String of(String word) {
        String w = word.toLowerCase();
        return w.substring(0, Math.min(w.length(), w.length() >= 6 ? 4 : 3));
    }

    public static boolean matches(String label, String stem) {
        if (label == null || stem == null || stem.isBlank()) return false;
        for (String token : label.toLowerCase().split("[^\\p{L}]+"))
            if (!token.isEmpty() && token.startsWith(stem)) return true;
        return false;
    }

    public static String firstWord(String text) {
        String[] parts = text.strip().split("[^\\p{L}]+");
        return parts.length == 0 ? "" : parts[0];
    }
}
