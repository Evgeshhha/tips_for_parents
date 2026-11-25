package org.example.tips_for_parents.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.example.tips_for_parents.model.enums.FeedingType;

public record UpdateChildRequest(FeedingType feedingType, @Min(0) @Max(2000) Integer milkMl) {}
