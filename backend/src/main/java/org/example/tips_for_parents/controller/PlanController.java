package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.personal.PlanView;
import org.example.tips_for_parents.service.personal.PlanService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/children/{childId}/plan")
@Tag(name = "Персональный план")
public class PlanController {

    private final PlanService plans;

    @Operation(summary = "Активный персональный план: по месяцам итоги (потребность, молоко, цель) и примерные дни")
    @GetMapping
    public PlanView active(@PathVariable UUID childId) {
        return plans.getActive(childId);
    }

    @Operation(summary = "Пересчитать план вручную (создаёт новую версию)")
    @PostMapping("/regenerate")
    public PlanView regenerate(@PathVariable UUID childId) {
        return plans.regenerate(childId, "manual regenerate");
    }
}
