package org.example.tips_for_parents.model.sleep;

import org.example.tips_for_parents.model.card.TextFact;

import java.util.List;

/**
 * Общий (не персональный) план сна. month == null — все диапазоны.
 * rhythm — дословные абзацы источника о сне; notes — чего источник не содержит; warnings — проблемы с данными.
 */
public record SleepCard(Integer month, String sourceUrl, List<SleepBand> bands, List<TextFact> rhythm,
                        List<String> notes, List<String> warnings) {
}
