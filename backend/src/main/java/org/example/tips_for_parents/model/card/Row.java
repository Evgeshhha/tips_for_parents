package org.example.tips_for_parents.model.card;

import org.example.tips_for_parents.model.entity.FeedingFact;
import org.example.tips_for_parents.model.pdf.FeedingAgeCell;

import java.util.List;

public record Row(FeedingFact fact, FeedingAgeCell cell, String unit, boolean isNew, String addonStem,
                   List<String> notes) {
    public String label() {
        return fact.getValueText();
    }
}