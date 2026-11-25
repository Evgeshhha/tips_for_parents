package org.example.tips_for_parents.model.dto;

import org.example.tips_for_parents.model.entity.Child;
import org.example.tips_for_parents.model.enums.FeedingType;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public record ChildDto(UUID id, String nickname, LocalDate birthDate, int ageMonths, Double birthWeightKg,
                       FeedingType feedingType, Integer milkMl) {
    public static ChildDto of(Child c) {
        return new ChildDto(c.getId(), c.getNickname(), c.getBirthDate(),
                (int) Period.between(c.getBirthDate(), LocalDate.now()).toTotalMonths(),
                c.getBirthWeightKg(), c.getFeedingType(), c.getMilkMl());
    }
}
