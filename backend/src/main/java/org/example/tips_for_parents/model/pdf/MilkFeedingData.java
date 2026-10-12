package org.example.tips_for_parents.model.pdf;

import java.util.List;

public record MilkFeedingData(List<MilkComposition> milkCompositions, List<MilkVolumeLimit> milkVolumeLimits) {
}

