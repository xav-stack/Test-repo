package dev.cannons.pure.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Calls the ticket adapter only on 0->1 and 1->0 transitions. */
public final class TicketReferenceCounter<K> {
    private final Map<K, Integer> references = new HashMap<>();
    private final BiConsumer<K, Boolean> transition;

    public TicketReferenceCounter(BiConsumer<K, Boolean> transition) {
        this.transition = Objects.requireNonNull(transition, "transition");
    }

    public int acquire(K key) {
        int previous = references.getOrDefault(key, 0);
        int next = previous + 1;
        references.put(key, next);
        if (previous == 0) {
            transition.accept(key, true);
        }
        return next;
    }

    public int release(K key) {
        int previous = references.getOrDefault(key, 0);
        if (previous == 0) {
            return 0;
        }
        if (previous == 1) {
            references.remove(key);
            transition.accept(key, false);
            return 0;
        }
        references.put(key, previous - 1);
        return previous - 1;
    }

    public int count(K key) {
        return references.getOrDefault(key, 0);
    }

    public void clear() {
        for (K key : references.keySet()) {
            transition.accept(key, false);
        }
        references.clear();
    }
}
