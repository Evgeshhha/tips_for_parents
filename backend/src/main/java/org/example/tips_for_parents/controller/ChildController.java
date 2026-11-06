package org.example.tips_for_parents.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.entity.Child;
import org.example.tips_for_parents.model.entity.ChildProduct;
import org.example.tips_for_parents.model.enums.FeedingType;
import org.example.tips_for_parents.model.enums.ProductStatus;
import org.example.tips_for_parents.service.personal.ChildService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/children")
@Tag(name = "Профиль ребёнка")
public class ChildController {

    public record CreateChildRequest(@Size(max = 100) String nickname,
                                     @NotNull @PastOrPresent LocalDate birthDate,
                                     @DecimalMin("1.0") @DecimalMax("7.0") Double birthWeightKg,
                                     @NotNull FeedingType feedingType,
                                     @Min(0) @Max(2000) Integer milkMl) {}

    public record UpdateChildRequest(FeedingType feedingType, @Min(0) @Max(2000) Integer milkMl) {}

    public record WeightRequest(@NotNull @PastOrPresent LocalDate measuredOn,
                                @DecimalMin("2.0") @DecimalMax("20.0") double weightKg) {}

    public record ProductStatusRequest(@NotBlank String productCode, @NotNull ProductStatus status, LocalDate startedOn) {}

    public record ChildDto(UUID id, String nickname, LocalDate birthDate, int ageMonths, Double birthWeightKg,
                           FeedingType feedingType, Integer milkMl) {
        static ChildDto of(Child c) {
            return new ChildDto(c.getId(), c.getNickname(), c.getBirthDate(),
                    (int) Period.between(c.getBirthDate(), LocalDate.now()).toTotalMonths(),
                    c.getBirthWeightKg(), c.getFeedingType(), c.getMilkMl());
        }
    }

    public record ProductStatusDto(String productCode, ProductStatus status, LocalDate startedOn) {
        static ProductStatusDto of(ChildProduct p) {
            return new ProductStatusDto(p.getProductCode(), p.getStatus(), p.getStartedOn());
        }
    }

    private final ChildService service;

    @Operation(summary = "Создать профиль ребёнка и сразу построить и сохранить персональный план (версия 1)")
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

    @Operation(summary = "Изменить вид вскармливания (breast/formula) и/или объём молока в сутки; план пересчитывается")
    @PutMapping("/{id}")
    public ChildDto update(@PathVariable UUID id, @Valid @RequestBody UpdateChildRequest r) {
        return ChildDto.of(service.updateProfile(id, r.feedingType(), r.milkMl()));
    }

    @Operation(summary = "Добавить измерение массы; план пересчитывается с реальной массой вместо оценки по таблице 6.1")
    @PostMapping("/{id}/weights")
    @ResponseStatus(HttpStatus.CREATED)
    public void addWeight(@PathVariable UUID id, @Valid @RequestBody WeightRequest r) {
        service.addWeight(id, r.measuredOn(), r.weightKg());
    }

    @Operation(summary = "Статусы продуктов ребёнка")
    @GetMapping("/{id}/products")
    public List<ProductStatusDto> products(@PathVariable UUID id) {
        return service.listProducts(id).stream().map(ProductStatusDto::of).toList();
    }

    @Operation(summary = "Родитель задаёт статус продукта: NOT_STARTED, INTRODUCING, INTRODUCED, PAUSED, EXCLUDED",
            description = "productCode — ключ из GET /api/products. План пересчитывается: введённые продукты становятся привычными блюдами, "
                    + "PAUSED/EXCLUDED убираются из дней, БЖУ пересчитывается без них.")
    @PutMapping("/{id}/products")
    public ProductStatusDto setProduct(@PathVariable UUID id, @Valid @RequestBody ProductStatusRequest r) {
        return ProductStatusDto.of(service.setProductStatus(id, r.productCode(), r.status(), r.startedOn(), null));
    }
}
