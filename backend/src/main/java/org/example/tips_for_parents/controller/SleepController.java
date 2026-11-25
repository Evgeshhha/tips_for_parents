package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.sleep.SleepCard;
import org.example.tips_for_parents.service.sleep.SleepService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sleep")
@Tag(name = "План сна")
public class SleepController {
    private final SleepService service;

    @Operation(summary = "Общий план сна для детей до года",
            description = "Данные только из утверждённых фактов страницы НМИЦ здоровья детей: сколько раз и сколько спит днём, "
                    + "ночной сон, режим бодрствования, дословные абзацы о сне. В notes — чего в источнике нет, в warnings — проблемы с данными.")
    @GetMapping
    public SleepCard all() {
        return service.all();
    }

    @Operation(summary = "План сна для месяца жизни (0–12)",
            description = "Источник даёт диапазоны (0–2, 2–4, 4–6, 6–9, 9–12 мес). На границе (2, 4, 6, 9) подходят два диапазона — "
                    + "возвращаются оба, это отмечено в warnings.")
    @GetMapping("/{month}")
    public SleepCard one(@Parameter(description = "Возраст ребёнка в месяцах, 0–12") @PathVariable int month) {
        return service.forMonth(month);
    }
}
