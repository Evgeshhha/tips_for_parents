package org.example.tips_for_parents.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SourceDocumentDto {

    private Long id;

    @NotBlank(message = "URL документа обязателен")
    @Size(max = 2048, message = "URL не должен превышать 2048 символов")
    private String url;

    @NotBlank(message = "Хеш документа обязателен")
    @Size(min = 64, max = 64, message = "Хеш должен содержать 64 символа")
    private String contentHash;

    @NotNull(message = "Дата загрузки обязательна")
    private Instant fetchedAt;

    @NotBlank(message = "Текст документа обязателен")
    private String rawText;
}