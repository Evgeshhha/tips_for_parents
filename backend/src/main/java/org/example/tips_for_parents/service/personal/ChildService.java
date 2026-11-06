package org.example.tips_for_parents.service.personal;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.entity.Child;
import org.example.tips_for_parents.model.entity.ChildProduct;
import org.example.tips_for_parents.model.entity.WeightMeasurement;
import org.example.tips_for_parents.model.enums.FeedingType;
import org.example.tips_for_parents.model.enums.ProductStatus;
import org.example.tips_for_parents.repository.ChildProductRepository;
import org.example.tips_for_parents.repository.ChildRepository;
import org.example.tips_for_parents.repository.WeightMeasurementRepository;
import org.example.tips_for_parents.service.card.MonthCardService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository children;
    private final WeightMeasurementRepository weights;
    private final ChildProductRepository products;
    private final MonthCardService cards;
    private final PlanService plans;

    @Transactional
    public Child create(String nickname, LocalDate birthDate, Double birthWeightKg, FeedingType feedingType, Integer milkMl) {
        Child child = children.save(new Child(nickname, birthDate, birthWeightKg, feedingType, milkMl));
        plans.regenerate(child.getId(), "initial plan");
        return child;
    }

    @Transactional(readOnly = true)
    public Child get(UUID id) {
        return children.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ребёнок не найден"));
    }

    @Transactional
    public Child updateProfile(UUID id, FeedingType feedingType, Integer milkMl) {
        Child child = get(id);
        if (feedingType != null) child.setFeedingType(feedingType);
        if (milkMl != null) child.setMilkMl(milkMl);
        plans.regenerate(id, "profile updated");
        return child;
    }

    @Transactional
    public void addWeight(UUID id, LocalDate measuredOn, double weightKg) {
        Child child = get(id);
        weights.save(new WeightMeasurement(child, measuredOn, weightKg));
        plans.regenerate(id, "weight added");
    }

    /** Действие родителя: введён / пауза / исключён / вернуть. productCode — ключ из GET /api/products. */
    @Transactional
    public ChildProduct setProductStatus(UUID id, String productCode, ProductStatus status, LocalDate startedOn, String reason) {
        Child child = get(id);
        String key = MonthCardService.productKey(productCode);
        if (!cards.productCatalog().containsKey(key))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный продукт: " + productCode);

        ChildProduct cp = products.findByChildIdAndProductCode(id, key)
                .map(existing -> {
                    existing.update(status, startedOn);
                    return existing;
                })
                .orElseGet(() -> products.save(new ChildProduct(child, key, status, startedOn)));
        plans.regenerate(id, reason != null ? reason : "product " + key + " -> " + status);
        return cp;
    }

    @Transactional(readOnly = true)
    public List<ChildProduct> listProducts(UUID id) {
        get(id);
        return products.findByChildId(id);
    }
}
