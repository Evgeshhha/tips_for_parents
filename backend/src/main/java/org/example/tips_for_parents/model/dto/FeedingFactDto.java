package org.example.tips_for_parents.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.tips_for_parents.model.enums.ReviewStatus;

@Getter
@Setter
public class FeedingFactDto {

    private Long id;

    @NotNull(message = "ID блока обязателен")
    @Positive(message = "ID блока должен быть положительным")
    private Long blockId;

    @NotBlank(message = "Тип факта обязателен")
    @Size(max = 32, message = "Тип факта не должен превышать 32 символа")
    private String factType;

    private String subject;

    private String payload;

    private String valueText;

    private Integer valueMin;

    private Integer valueMax;

    @NotBlank(message = "Текст факта обязателен")
    private String sentence;

    @NotNull(message = "Статус проверки обязателен")
    private ReviewStatus reviewStatus;

    @Size(max = 8, message = "Уровень уверенности не должен превышать 8 символов")
    private String confidence;

    @Size(max = 8, message = "Имя проверяющего не должно превышать 8 символов")
    private String reviewedBy;

    private String reviewNote;
}