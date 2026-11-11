package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.service.card.MonthCardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
@Tag(name = "Профиль ребёнка")
public class ProductController {

    public record ProductDto(String code, String label) {}

    private final MonthCardService cards;

    @Operation(summary = "Каталог продуктов таблицы 5.1 (утверждённые факты): code для запросов, label для показа")
    @GetMapping
    public List<ProductDto> all() {
        return cards.productCatalog().entrySet().stream()
                .map(e -> new ProductDto(e.getKey(), e.getValue())).toList();
    }
}
