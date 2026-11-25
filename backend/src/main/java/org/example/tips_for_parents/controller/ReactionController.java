package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.enums.ReactionAction;
import org.example.tips_for_parents.model.personal.ReactionView;
import org.example.tips_for_parents.service.personal.ReactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/children/{childId}/reactions")
@Tag(name = "Реакции на прикорм")
public class ReactionController {

    public record ReactionRequest(@NotBlank @Size(max = 2000) String text, String productCode) {}

    public record ConfirmRequest(ReactionAction action) {}

    private final ReactionService service;

    @Operation(summary = "Описать реакцию ребёнка: разбор текста (ИИ или словарь) возвращает ПРЕДЛОЖЕНИЕ",
            description = "План не меняется, пока родитель не подтвердит. productCode — необязательная подсказка (ключ из GET /api/products). "
                    + "Срочность — максимум из ответа ИИ и правил (затруднённое дыхание, отёк и т. п.). Это не диагноз.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReactionView submit(@PathVariable UUID childId, @Valid @RequestBody ReactionRequest r) {
        return service.submit(childId, r.text(), r.productCode());
    }

    @Operation(summary = "История сообщений о реакциях")
    @GetMapping
    public List<ReactionView> list(@PathVariable UUID childId) {
        return service.list(childId);
    }

    @Operation(summary = "Подтвердить действие (по умолчанию предложенное): продукт уходит на паузу, план пересчитывается")
    @PostMapping("/{reactionId}/confirm")
    public ReactionView confirm(@PathVariable UUID childId, @PathVariable UUID reactionId,
                                @RequestBody(required = false) ConfirmRequest r) {
        return service.confirm(childId, reactionId, r == null ? null : r.action());
    }

    @Operation(summary = "Отклонить предложение: план не меняется")
    @PostMapping("/{reactionId}/reject")
    public ReactionView reject(@PathVariable UUID childId, @PathVariable UUID reactionId) {
        return service.reject(childId, reactionId);
    }
}
