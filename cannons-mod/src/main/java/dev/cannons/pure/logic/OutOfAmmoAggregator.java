package dev.cannons.pure.logic;

import dev.cannons.pure.model.CannonId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Batches empty-cannon notices once per starter per server tick. */
public final class OutOfAmmoAggregator {
    public record Notice(int count, CannonId onlyCannon) {
    }

    private final Map<UUID, List<CannonId>> pending = new HashMap<>();

    public void record(UUID starter, CannonId cannon) {
        pending.computeIfAbsent(starter, ignored -> new ArrayList<>()).add(cannon);
    }

    public Map<UUID, Notice> drainTick() {
        Map<UUID, Notice> notices = new HashMap<>();
        pending.forEach((starter, cannons) -> notices.put(
                starter,
                new Notice(cannons.size(), cannons.size() == 1 ? cannons.get(0) : null)
        ));
        pending.clear();
        return Map.copyOf(notices);
    }
}
