package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.value.Nutrients;

public record MilkInfo(String kind, String label, double volumeMl, String volumeSource, Nutrients per100ml,
                       Nutrients perDay, String compositionSource) {
}
