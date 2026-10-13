package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.card.MonthCard;
import org.example.tips_for_parents.service.card.MonthCardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/months")
@Tag(name = "Карточки месяцев")
public class MonthController {
    private final MonthCardService service;

    @Operation(summary = "Карточки всех месяцев (по умолчанию с 6 до 12)")
    @GetMapping
    public List<MonthCard> all(
            @Parameter(description = "Масса ребёнка, кг (по умолчанию — оценка по таблице 6.1)") @RequestParam(required = false) Double weightKg,
            @Parameter(description = "Масса при рождении, кг, для оценки массы (по умолчанию cards.birth-weight-kg)") @RequestParam(required = false) Double birthWeightKg,
            @Parameter(description = "Чем питается ребёнок: breast (грудное молоко) или formula (смесь 1)") @RequestParam(required = false, defaultValue = "breast") String milk,
            @Parameter(description = "Объём молока/смеси в сутки, мл (по умолчанию — верхняя граница программы)") @RequestParam(required = false) Double milkMl) {
        return service.allCards(weightKg, birthWeightKg, milk, milkMl);
    }

    @Operation(summary = "Карточка месяца: что разрешено, потребность, молоко/смесь, примерные дни (по одному на новый продукт), правила, ограничения",
            description = "Данные только из утверждённых фактов PDF. Месяцы 9–12 в таблице 5.1 — один столбец (поле column). "
                    + "Потребность = нормы на кг (приложение 1) × масса; из неё вычитается молоко/смесь; "
                    + "граммовки блюд рассчитываются по БЖУ (таблица 7.4) на остаток. В warnings — нехватка данных.")
    @GetMapping("/{month}")
    public MonthCard one(
            @Parameter(description = "Возраст ребёнка в месяцах, 4–12") @PathVariable int month,
            @Parameter(description = "Масса ребёнка, кг") @RequestParam(required = false) Double weightKg,
            @Parameter(description = "Масса при рождении, кг") @RequestParam(required = false) Double birthWeightKg,
            @Parameter(description = "breast (грудное молоко) или formula (смесь 1)") @RequestParam(required = false, defaultValue = "breast") String milk,
            @Parameter(description = "Объём молока/смеси в сутки, мл (по умолчанию — верхняя граница программы)") @RequestParam(required = false) Double milkMl) {
        return service.card(month, weightKg, birthWeightKg, milk, milkMl);
    }
}
