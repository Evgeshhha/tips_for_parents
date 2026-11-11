package org.example.tips_for_parents.service.llm;

import org.example.tips_for_parents.model.enums.ReactionAction;
import org.example.tips_for_parents.model.enums.Symptom;
import org.example.tips_for_parents.model.enums.Urgency;
import org.example.tips_for_parents.model.personal.ReactionAnalysis;
import org.example.tips_for_parents.model.personal.ReactionContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Запасной разбор без ИИ (LLM выключен или недоступен): простые основы слов.
 * Регулярки можно заменить лемматизацией через ваш Stanza-сервис.
 */
@Component
public class KeywordReactionAnalyzer {

    private static final Map<Symptom, Pattern> DICT = new LinkedHashMap<>();

    private static void put(Symptom s, String regex) {
        DICT.put(s, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }

    static {
        put(Symptom.RASH, "сып|высыпан|пятн");
        put(Symptom.REDNESS, "покраснен|красн");
        put(Symptom.HIVES, "крапивниц");
        put(Symptom.ITCHING, "зуд|чешет|чешут");
        put(Symptom.SWELLING, "от[её]к|припух|опух");
        put(Symptom.VOMITING, "рвот|рвёт|рвет|стошн|срыгива");
        put(Symptom.DIARRHEA, "понос|жидк\\p{L}*\\s+стул|диаре");
        put(Symptom.CONSTIPATION, "запор|нет\\s+стула");
        put(Symptom.COLIC, "колик");
        put(Symptom.BLOATING, "вздут|газик|газов");
        put(Symptom.FOOD_REFUSAL, "отказ\\p{L}*\\s+(от\\s+)?(еды|есть|кушать|пить)|не\\s+хочет\\s+есть");
        put(Symptom.FUSSINESS, "капризн|плачет|беспокоен|беспокойн");
        put(Symptom.BREATHING_DIFFICULTY, "задыха|тяжело\\s+дыш|хрип");
        put(Symptom.LETHARGY, "вял|сонлив");
    }

    public ReactionAnalysis analyze(ReactionContext ctx, String text) {
        List<Symptom> found = new ArrayList<>();
        DICT.forEach((s, p) -> {
            if (p.matcher(text).find()) found.add(s);
        });

        Urgency urgency = RedFlagRules.evaluate(text);
        if (urgency == Urgency.NONE && !found.isEmpty()) urgency = Urgency.MONITOR;

        String product = ctx.productHint();
        ReactionAction action;
        String question = null;
        if (found.isEmpty()) {
            action = ReactionAction.ASK_CLARIFICATION;
            question = "Опишите, пожалуйста, что именно вы заметили и через какое время после еды.";
        } else if (product != null) {
            action = ReactionAction.PAUSE_PRODUCT;
        } else {
            action = ReactionAction.ASK_CLARIFICATION;
            question = "После какого продукта появились эти симптомы?";
        }

        String summary = found.isEmpty()
                ? "Автоматический разбор без ИИ: симптомы распознать не удалось."
                : "Автоматический разбор без ИИ. Замечено: "
                  + found.stream().map(Enum::name).collect(Collectors.joining(", ")) + ".";

        return new ReactionAnalysis(product, found, null, urgency, action, question, summary, false);
    }
}
