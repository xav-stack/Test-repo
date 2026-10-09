package dev.cannons.pure.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Aggregates continuous-fire refusal notices per starter. */
public final class BlockedNoticeThrottle {
    private static final class Pending {
        private int count;
        private long lastSentTick;
        private boolean sentBefore;
    }

    private final Map<UUID, Pending> pending = new HashMap<>();

    public void record(UUID starter) {
        pending.computeIfAbsent(starter, ignored -> new Pending()).count++;
    }

    public Map<UUID, Integer> flushReady(long tick, long throttleTicks) {
        Map<UUID, Integer> ready = new HashMap<>();
        pending.entrySet().removeIf(entry -> {
            Pending value = entry.getValue();
            if (value.count == 0) {
                return false;
            }
            if (!value.sentBefore || tick - value.lastSentTick >= throttleTicks) {
                ready.put(entry.getKey(), value.count);
                value.count = 0;
                value.lastSentTick = tick;
                value.sentBefore = true;
            }
            return false;
        });
        return Map.copyOf(ready);
    }
}
