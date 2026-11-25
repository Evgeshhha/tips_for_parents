package org.example.tips_for_parents.model.card;

import java.util.Set;

public record ChildContext(Set<String> introduced, Set<String> pending, Set<String> unavailable) {
    public static final ChildContext NONE = new ChildContext(Set.of(), Set.of(), Set.of());
}
