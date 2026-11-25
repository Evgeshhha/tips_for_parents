package org.example.tips_for_parents.model.sleep;

import java.util.List;

public record ParsedSleepTable(List<SleepRow> rows, List<String> skipped) {
}
