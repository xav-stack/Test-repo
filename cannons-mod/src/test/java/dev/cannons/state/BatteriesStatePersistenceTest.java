package dev.cannons.state;

import dev.cannons.pure.model.CannonId;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BatteriesStatePersistenceTest {
    private static final UUID PLAYER =
            UUID.fromString("178b1665-72a7-4aad-8568-5d29d04fb6df");
    private static final CannonId CANNON =
            new CannonId("minecraft:the_nether", -17, 63, 32);

    @Test
    void batteryNamesMembersAndDimensionsSurviveNbtRoundTrip() {
        BatteriesState original = new BatteriesState();
        assertTrue(original.add(PLAYER, "Main_Base", CANNON));
        NbtCompound serialized = original.writeNbt(new NbtCompound());

        BatteriesState loaded = BatteriesState.fromNbt(serialized);
        assertEquals(Set.of(CANNON), loaded.members(PLAYER, "Main_Base"));
        assertEquals(1, loaded.list(PLAYER).get("Main_Base"));
        assertEquals(Set.of(PLAYER), loaded.owners(CANNON));
    }

    @Test
    void malformedEntriesAndBlankDimensionAreSkippedWithoutLosingValidMembers() {
        NbtCompound root = new NbtCompound();
        NbtList players = new NbtList();
        NbtCompound player = new NbtCompound();
        player.putUuid("Uuid", PLAYER);
        NbtList batteries = new NbtList();

        NbtCompound validBattery = new NbtCompound();
        validBattery.putString("Name", "Valid");
        NbtList members = new NbtList();
        NbtCompound missingDimension = new NbtCompound();
        missingDimension.putInt("X", 1);
        members.add(missingDimension);
        NbtCompound validMember = new NbtCompound();
        validMember.putString("Dimension", "minecraft:overworld");
        validMember.putInt("X", -1);
        validMember.putInt("Y", 64);
        validMember.putInt("Z", -17);
        members.add(validMember);
        validBattery.put("Members", members);
        batteries.add(validBattery);

        NbtCompound invalidName = new NbtCompound();
        invalidName.putString("Name", "bad-name");
        batteries.add(invalidName);
        player.put("Batteries", batteries);
        players.add(player);
        root.put("Players", players);

        BatteriesState loaded = assertDoesNotThrow(() -> BatteriesState.fromNbt(root));
        CannonId validId = new CannonId("minecraft:overworld", -1, 64, -17);
        assertEquals(Set.of(validId), loaded.members(PLAYER, "Valid"));
        assertNull(loaded.members(PLAYER, "bad-name"));
    }
}
