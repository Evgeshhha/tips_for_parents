package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.dto.ProductStatusDto;
import org.example.tips_for_parents.model.request.ProductStatusRequest;
import org.example.tips_for_parents.service.card.MonthCardService;
import org.example.tips_for_parents.service.personal.ChildService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Tag(name = "Продукты")
public class ProductController {

    public record ProductDto(String code, String label) {}
    private final MonthCardService cards;
    private final ChildService service;

    @Operation(summary = "Каталог продуктов таблицы 5.1: code для запросов, label для показа")
    @GetMapping("/products")
    public List<ProductDto> all() {
        return cards.productCatalog().entrySet().stream()
                .map(e -> new ProductDto(e.getKey(), e.getValue())).toList();
    }

    @Operation(summary = "Статусы продуктов ребёнка")
    @GetMapping("/children/{id}/products")
    public List<ProductStatusDto> products(@PathVariable UUID id) {
        return service.listProducts(id).stream().map(ProductStatusDto::of).toList();
    }

    @Operation(summary = "Родитель задаёт статус продукта: NOT_STARTED, INTRODUCING, INTRODUCED, PAUSED, EXCLUDED",
            description = "productCode — ключ из GET /api/products. План пересчитывается: введённые продукты становятся привычными блюдами, "
                    + "PAUSED/EXCLUDED убираются из дней, БЖУ пересчитывается без них.")
    @PutMapping("/children/{id}/products")
    public ProductStatusDto setProduct(@PathVariable UUID id, @Valid @RequestBody ProductStatusRequest r) {
        return ProductStatusDto.of(service.setProductStatus(id, r.productCode(), r.status(), r.startedOn(), null));
    }
}
