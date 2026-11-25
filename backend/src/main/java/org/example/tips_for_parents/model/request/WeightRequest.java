package org.example.tips_for_parents.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record WeightRequest(@NotNull @PastOrPresent LocalDate measuredOn,
                            @DecimalMin("2.0") @DecimalMax("20.0") double weightKg) {}
