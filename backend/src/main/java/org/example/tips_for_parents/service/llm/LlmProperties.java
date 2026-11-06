package org.example.tips_for_parents.service.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Подхватывается @ConfigurationPropertiesScan из TipsForParentsApplication. */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://api.anthropic.com") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("claude-sonnet-5-5") String model,
        @DefaultValue("20000") int timeoutMs) {
}
