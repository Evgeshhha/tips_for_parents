package org.example.tips_for_parents.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.tips_for_parents.model.enums.ProductStatus;

import java.time.LocalDate;

public record ProductStatusRequest(@NotBlank String productCode, @NotNull ProductStatus status, LocalDate startedOn) {}
