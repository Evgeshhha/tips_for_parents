package org.example.tips_for_parents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "cards")
public record CardsProperties(
        @DefaultValue("4") int firstMonth,
        @DefaultValue("12") int lastMonth,
        @DefaultValue("3.3") double birthWeightKg,
        @DefaultValue("0.25") double minPortionShare) {
}
