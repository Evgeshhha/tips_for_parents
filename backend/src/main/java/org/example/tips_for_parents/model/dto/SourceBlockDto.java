package org.example.tips_for_parents.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceBlockDto {

    private Long id;

    @Positive(message = "Позиция должна быть положительной")
    private int pos;

    @Size(max = 500, message = "Заголовок не должен превышать 500 символов")
    private String heading;

    @NotBlank(message = "Текст блока обязателен")
    private String text;

    @NotNull(message = "ID документа обязателен")
    @Positive(message = "ID документа должен быть положительным")
    private Long documentId;
}