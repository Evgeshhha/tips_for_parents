package org.example.tips_for_parents.model.personal;

import org.example.tips_for_parents.model.enums.ProductStatus;

import java.util.Map;
import java.util.Set;

/** Всё, что уходит в ИИ о ребёнке: только возраст в месяцах и статусы продуктов. Без имени и даты рождения. */
public record ReactionContext(int ageMonths,
                              Set<String> allowedProductCodes,
                              Map<String, ProductStatus> statuses,
                              String productHint) {
}
