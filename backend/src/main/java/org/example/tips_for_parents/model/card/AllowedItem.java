package org.example.tips_for_parents.model.card;

import java.util.List;

public record AllowedItem(String name, String amount, boolean newInPeriod, List<String> notes, long factId) {
}
