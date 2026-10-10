package dev.cannons.state;

import dev.cannons.pure.logic.Batteries;
import dev.cannons.pure.model.CannonId;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Set;

public final class BatteriesState extends PersistentState {
    public static final String KEY = "cannons_batteries";
    private final Batteries batteries = new Batteries();

    public static BatteriesState get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager()
                .getOrCreate(BatteriesState::fromNbt, BatteriesState::new, KEY);
    }

    public static BatteriesState fromNbt(NbtCompound nbt) {
        BatteriesState state = new BatteriesState();
        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int p = 0; p < players.size(); p++) {
            NbtCompound player = players.getCompound(p);
            if (!player.containsUuid("Uuid")) {
                continue;
            }
            UUID playerId = player.getUuid("Uuid");
            NbtList namedBatteries = player.getList("Batteries", NbtElement.COMPOUND_TYPE);
            for (int b = 0; b < namedBatteries.size(); b++) {
                NbtCompound battery = namedBatteries.getCompound(b);
                String name = battery.getString("Name");
                if (!Batteries.isValidName(name, 16)) {
                    continue;
                }
                NbtList members = battery.getList("Members", NbtElement.COMPOUND_TYPE);
                for (int c = 0; c < members.size(); c++) {
                    NbtCompound member = members.getCompound(c);
                    String dimension = member.getString("Dimension");
                    if (dimension.isBlank()) {
                        continue;
                    }
                    state.batteries.add(playerId, name, new CannonId(
                            dimension,
                            member.getInt("X"),
                            member.getInt("Y"),
                            member.getInt("Z")
                    ), 16);
                }
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList players = new NbtList();
        batteries.snapshot().forEach((uuid, namedBatteries) -> {
            NbtCompound player = new NbtCompound();
            player.putUuid("Uuid", uuid);
            NbtList list = new NbtList();
            namedBatteries.forEach((name, ids) -> {
                NbtCompound battery = new NbtCompound();
                battery.putString("Name", name);
                NbtList members = new NbtList();
                ids.forEach(id -> {
                    NbtCompound member = new NbtCompound();
                    member.putString("Dimension", id.dimension());
                    member.putInt("X", id.x());
                    member.putInt("Y", id.y());
                    member.putInt("Z", id.z());
                    members.add(member);
                });
                battery.put("Members", members);
                list.add(battery);
            });
            player.put("Batteries", list);
            players.add(player);
        });
        nbt.put("Players", players);
        return nbt;
    }

    public boolean add(UUID player, String name, CannonId id) {
        boolean changed = batteries.add(player, name, id, 16);
        if (changed) markDirty();
        return changed;
    }

    public boolean remove(UUID player, String name, CannonId id) {
        boolean changed = batteries.remove(player, name, id, 16);
        if (changed) markDirty();
        return changed;
    }

    public int pruneOnUnbind(UUID player, CannonId id) {
        int removed = batteries.pruneOnUnbind(player, id);
        if (removed > 0) markDirty();
        return removed;
    }

    public int removeCannonEverywhere(CannonId id) {
        int removed = batteries.removeCannonEverywhere(id);
        if (removed > 0) markDirty();
        return removed;
    }

    public Set<CannonId> members(UUID player, String name) {
        return batteries.members(player, name);
    }

    public Map<String, Integer> list(UUID player) {
        return batteries.list(player);
    }

    public Set<UUID> owners(CannonId id) {
        return batteries.owners(id);
    }
}
