package dev.cannons.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CannonRegistryState extends PersistentState {
    public static final String KEY = "cannons_registry";

    private static final class Entry {
        private UUID label;

        private Entry(UUID label) {
            this.label = label;
        }
    }

    private final Map<Long, Entry> cannons = new HashMap<>();
    private final Map<Long, Set<Long>> byChunk = new HashMap<>();

    public static CannonRegistryState get(net.minecraft.server.world.ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                CannonRegistryState::fromNbt,
                CannonRegistryState::new,
                KEY
        );
    }

    public static CannonRegistryState fromNbt(NbtCompound nbt) {
        CannonRegistryState state = new CannonRegistryState();
        NbtList list = nbt.getList("Cannons", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            UUID label = entry.containsUuid("Label") ? entry.getUuid("Label") : null;
            state.add(entry.getLong("Pos"), new Entry(label));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        cannons.forEach((pos, entry) -> {
            NbtCompound serialized = new NbtCompound();
            serialized.putLong("Pos", pos);
            if (entry.label != null) {
                serialized.putUuid("Label", entry.label);
            }
            list.add(serialized);
        });
        nbt.put("Cannons", list);
        return nbt;
    }

    public boolean contains(BlockPos pos) {
        return cannons.containsKey(pos.asLong());
    }

    public boolean register(BlockPos pos) {
        if (cannons.containsKey(pos.asLong())) {
            return false;
        }
        add(pos.asLong(), new Entry(null));
        markDirty();
        return true;
    }

    public boolean unregister(BlockPos pos) {
        Entry removed = cannons.remove(pos.asLong());
        if (removed == null) {
            return false;
        }
        long chunk = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        Set<Long> chunkPositions = byChunk.get(chunk);
        if (chunkPositions != null) {
            chunkPositions.remove(pos.asLong());
            if (chunkPositions.isEmpty()) byChunk.remove(chunk);
        }
        markDirty();
        return true;
    }

    public UUID labelUuid(BlockPos pos) {
        Entry entry = cannons.get(pos.asLong());
        return entry == null ? null : entry.label;
    }

    public void setLabelUuid(BlockPos pos, UUID uuid) {
        Entry entry = cannons.get(pos.asLong());
        if (entry != null && !java.util.Objects.equals(entry.label, uuid)) {
            entry.label = uuid;
            markDirty();
        }
    }

    public List<BlockPos> positions() {
        List<BlockPos> result = new ArrayList<>(cannons.size());
        cannons.keySet().forEach(pos -> result.add(BlockPos.fromLong(pos)));
        return result;
    }

    public Collection<BlockPos> positionsInChunk(ChunkPos chunk) {
        List<BlockPos> result = new ArrayList<>();
        Set<Long> indexed = byChunk.get(ChunkPos.toLong(chunk.x, chunk.z));
        if (indexed != null) indexed.forEach(packed -> result.add(BlockPos.fromLong(packed)));
        return result;
    }

    private void add(long packed, Entry entry) {
        BlockPos pos = BlockPos.fromLong(packed);
        cannons.put(packed, entry);
        byChunk.computeIfAbsent(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4),
                ignored -> new LinkedHashSet<>()).add(packed);
    }
}
