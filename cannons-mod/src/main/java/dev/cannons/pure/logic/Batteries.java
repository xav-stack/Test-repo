package dev.cannons.pure.logic;

import dev.cannons.pure.model.CannonId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Player-owned named cannon selections. */
public final class Batteries {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z_]{1,16}");
    private final Map<UUID, Map<String, LinkedHashSet<CannonId>>> players = new LinkedHashMap<>();

    public static boolean isValidName(String name, int maxLength) {
        return name != null && name.length() <= Math.min(16, maxLength)
                && VALID_NAME.matcher(name).matches();
    }

    public boolean add(UUID player, String name, CannonId cannon, int maxLength) {
        requireName(name, maxLength);
        return players.computeIfAbsent(player, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(name, ignored -> new LinkedHashSet<>())
                .add(cannon);
    }

    public boolean remove(UUID player, String name, CannonId cannon, int maxLength) {
        requireName(name, maxLength);
        Map<String, LinkedHashSet<CannonId>> batteries = players.get(player);
        if (batteries == null) {
            return false;
        }
        Set<CannonId> members = batteries.get(name);
        if (members == null || !members.remove(cannon)) {
            return false;
        }
        removeEmptyBatteries(player, batteries);
        return true;
    }

    /** User-requested behavior: losing a bind removes that cannon from this player's batteries. */
    public int pruneOnUnbind(UUID player, CannonId cannon) {
        Map<String, LinkedHashSet<CannonId>> batteries = players.get(player);
        if (batteries == null) {
            return 0;
        }
        int removed = 0;
        for (Set<CannonId> members : batteries.values()) {
            if (members.remove(cannon)) {
                removed++;
            }
        }
        removeEmptyBatteries(player, batteries);
        return removed;
    }

    public int removeCannonEverywhere(CannonId cannon) {
        int removed = 0;
        for (Map.Entry<UUID, Map<String, LinkedHashSet<CannonId>>> player : players.entrySet()) {
            for (Set<CannonId> members : player.getValue().values()) {
                if (members.remove(cannon)) {
                    removed++;
                }
            }
        }
        for (Map.Entry<UUID, Map<String, LinkedHashSet<CannonId>>> player
                : new ArrayList<>(players.entrySet())) {
            removeEmptyBatteries(player.getKey(), player.getValue());
        }
        return removed;
    }

    public Set<CannonId> members(UUID player, String name) {
        Map<String, LinkedHashSet<CannonId>> batteries = players.get(player);
        if (batteries == null || !batteries.containsKey(name)) {
            return null;
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(batteries.get(name)));
    }

    public Map<String, Integer> list(UUID player) {
        Map<String, LinkedHashSet<CannonId>> batteries = players.get(player);
        if (batteries == null) {
            return Map.of();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        batteries.forEach((name, members) -> result.put(name, members.size()));
        return Collections.unmodifiableMap(result);
    }

    public Set<UUID> owners(CannonId cannon) {
        Set<UUID> result = new LinkedHashSet<>();
        players.forEach((player, batteries) -> {
            if (batteries.values().stream().anyMatch(members -> members.contains(cannon))) {
                result.add(player);
            }
        });
        return Collections.unmodifiableSet(result);
    }

    public Map<UUID, Map<String, Set<CannonId>>> snapshot() {
        Map<UUID, Map<String, Set<CannonId>>> result = new LinkedHashMap<>();
        players.forEach((player, batteries) -> {
            Map<String, Set<CannonId>> values = new LinkedHashMap<>();
            batteries.forEach((name, members) ->
                    values.put(name, Collections.unmodifiableSet(new LinkedHashSet<>(members))));
            result.put(player, Collections.unmodifiableMap(values));
        });
        return Collections.unmodifiableMap(result);
    }

    private void requireName(String name, int maxLength) {
        if (!isValidName(name, maxLength)) {
            throw new IllegalArgumentException("Invalid battery name");
        }
    }

    private void removeEmptyBatteries(
            UUID player,
            Map<String, LinkedHashSet<CannonId>> batteries
    ) {
        batteries.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (batteries.isEmpty()) {
            players.remove(player);
        }
    }
}
