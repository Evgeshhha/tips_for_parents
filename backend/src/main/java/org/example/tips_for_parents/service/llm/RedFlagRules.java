package org.example.tips_for_parents.service.llm;

import org.example.tips_for_parents.model.enums.Urgency;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Детерминированная страховка: опасные признаки определяются правилами независимо от ответа ИИ.
 * Итоговая срочность = максимум из правил и ИИ.
 */
public final class RedFlagRules {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final List<Pattern> EMERGENCY = List.of(
            Pattern.compile("задыха|тяжело\\s+дыш|затруднен\\p{L}*\\s+дыхан|не\\s+дышит|хрип|посинел|синюш|"
                    + "без\\s+сознан|потерял\\p{L}*\\s+сознан|судорог|"
                    + "от[её]к\\p{L}*\\s+(лица|губ|язык|горл)", FLAGS));

    private static final List<Pattern> SEE_DOCTOR = List.of(
            Pattern.compile("кров\\p{L}*\\s+(в|на)\\s+стул|кровав|многократн\\p{L}*\\s+рвот|повторн\\p{L}*\\s+рвот|"
                    + "вял|температур|обезвожив|крапивниц|сильн\\p{L}*\\s+(сып|зуд)", FLAGS));

    private RedFlagRules() {
    }

    public static Urgency evaluate(String text) {
        if (text == null) return Urgency.NONE;
        if (EMERGENCY.stream().anyMatch(p -> p.matcher(text).find())) return Urgency.EMERGENCY;
        if (SEE_DOCTOR.stream().anyMatch(p -> p.matcher(text).find())) return Urgency.SEE_DOCTOR;
        return Urgency.NONE;
    }

    /** Нейтральный текст для родителя; не диагноз и не рекомендация по лечению. */
    public static String notice(Urgency u) {
        return switch (u) {
            case EMERGENCY -> "Описаны тревожные признаки. Немедленно вызовите скорую помощь (103 или 112) — не ждите.";
            case SEE_DOCTOR -> "Рекомендуем как можно скорее показать ребёнка педиатру и не вводить новые продукты до консультации.";
            case MONITOR -> "Понаблюдайте за состоянием. Если симптомы усилятся или появятся новые, обратитесь к педиатру.";
            case NONE -> null;
        };
    }
}
