package org.example.tips_for_parents.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.example.tips_for_parents.model.enums.FeedingType;

import java.time.LocalDate;

public record CreateChildRequest(@Size(max = 100) String nickname,
                                 @NotNull @PastOrPresent LocalDate birthDate,
                                 @DecimalMin("1.0") @DecimalMax("7.0") Double birthWeightKg,
                                 @NotNull FeedingType feedingType,
                                 @Min(0) @Max(2000) Integer milkMl) {}
