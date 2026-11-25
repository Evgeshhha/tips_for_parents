package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.dto.ChildDto;
import org.example.tips_for_parents.model.request.CreateChildRequest;
import org.example.tips_for_parents.model.request.UpdateChildRequest;
import org.example.tips_for_parents.model.request.WeightRequest;
import org.example.tips_for_parents.service.personal.ChildService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/children")
@Tag(name = "Профиль ребёнка")
public class ChildController {
    private final ChildService service;

    @Operation(summary = "Создать профиль ребёнка и персональный план")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChildDto create(@Valid @RequestBody CreateChildRequest r) {
        return ChildDto.of(service.create(r.nickname(), r.birthDate(), r.birthWeightKg(), r.feedingType(), r.milkMl()));
    }

    @Operation(summary = "Профиль ребёнка")
    @GetMapping("/{id}")
    public ChildDto get(@PathVariable UUID id) {
        return ChildDto.of(service.get(id));
    }

    @Operation(summary = "Изменить вид вскармливания и/или объём молока в сутки")
    @PutMapping("/{id}")
    public ChildDto update(@PathVariable UUID id, @Valid @RequestBody UpdateChildRequest r) {
        return ChildDto.of(service.updateProfile(id, r.feedingType(), r.milkMl()));
    }

    @Operation(summary = "Добавить измерение массы")
    @PostMapping("/{id}/weights")
    @ResponseStatus(HttpStatus.CREATED)
    public void addWeight(@PathVariable UUID id, @Valid @RequestBody WeightRequest r) {
        service.addWeight(id, r.measuredOn(), r.weightKg());
    }
}
