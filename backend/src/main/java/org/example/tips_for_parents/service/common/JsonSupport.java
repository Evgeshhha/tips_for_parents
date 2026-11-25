package org.example.tips_for_parents.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Общие операции с JSON-payload фактов: запись и «мягкое» чтение. */
@Component
@RequiredArgsConstructor
public class JsonSupport {
    private final ObjectMapper om;

    public String toJson(Object value) {
        try {
            return om.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** null, если json == null или не читается как type. */
    public <T> T readOrNull(String json, Class<T> type) {
        if (json == null) return null;
        try {
            return om.readValue(json, type);
        } catch (IOException e) {
            return null;
        }
    }
}
