package org.example.tips_for_parents.service.personal;

import java.time.LocalDate;
import java.time.Period;

final class Ages {
    private Ages() {
    }

    static int months(LocalDate birthDate, LocalDate today) {
        return (int) Period.between(birthDate, today).toTotalMonths();
    }

    /** Карточки месяцев работают в диапазоне cards.first-month … cards.last-month (по умолчанию 4–12). */
    static int clamp(int month, int first, int last) {
        return Math.max(first, Math.min(last, month));
    }
}
