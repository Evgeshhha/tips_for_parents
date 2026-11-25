package org.example.tips_for_parents.service.common;

import org.example.tips_for_parents.exception.ApiErrors;

/** Проверка номера месяца в запросе: общая для карточек прикорма и плана сна. */
public final class MonthValidator {
    private MonthValidator() {
    }

    public static void require(int month, int min, int max) {
        if (month < min || month > max)
            throw ApiErrors.badRequest("Месяц должен быть от " + min + " до " + max);
    }
}
