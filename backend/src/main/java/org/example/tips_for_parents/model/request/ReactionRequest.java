package org.example.tips_for_parents.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(@NotBlank @Size(max = 2000) String text, String productCode) {}
