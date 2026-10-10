package dev.cannons.pure;

import dev.cannons.pure.logic.Batteries;
import dev.cannons.pure.logic.SlotRules;
import dev.cannons.pure.model.CannonId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BatteriesAndSlotsTest {
    private static final UUID PLAYER = UUID.fromString("d3122dc2-3457-4f99-b584-0d8cc484f5a0");
    private static final CannonId CANNON = new CannonId("minecraft:overworld", 4, 64, 8);

    @Test
    void batteryNamesAreValidatedAndCaseSensitive() {
        assertTrue(Batteries.isValidName("Main_Base", 16));
        assertTrue(Batteries.isValidName("_", 16));
        assertFalse(Batteries.isValidName("main1", 16));
        assertFalse(Batteries.isValidName("bad-name", 16));
        assertFalse(Batteries.isValidName("SeventeenCharsHere", 16));
        assertFalse(Batteries.isValidName("LongName", 4));

        Batteries batteries = new Batteries();
        batteries.add(PLAYER, "Main", CANNON, 16);
        assertEquals(Set.of(CANNON), batteries.members(PLAYER, "Main"));
        assertNull(batteries.members(PLAYER, "main"));
    }

    @Test
    void membershipIsPrunedOnUnbindAndEmptyBatteriesAreDeleted() {
        Batteries batteries = new Batteries();
        batteries.add(PLAYER, "A", CANNON, 16);
        batteries.add(PLAYER, "B", CANNON, 16);
        assertEquals(2, batteries.pruneOnUnbind(PLAYER, CANNON));
        assertTrue(batteries.list(PLAYER).isEmpty());

        batteries.add(PLAYER, "A", CANNON, 16);
        assertEquals(1, batteries.removeCannonEverywhere(CANNON));
        assertEquals(Map.of(), batteries.list(PLAYER));
    }

    @Test
    void slotRulesAndNameBindingMatchTheInventoryContract() {
        SlotRules.Item tnt = new SlotRules.Item(SlotRules.ItemKind.TNT, null);
        SlotRules.Item named = new SlotRules.Item(SlotRules.ItemKind.OTHER, "PlayerOne");
        SlotRules.Item emerald = new SlotRules.Item(SlotRules.ItemKind.EMERALD, null);
        assertTrue(SlotRules.accepts(0, tnt));
        assertFalse(SlotRules.accepts(0, emerald));
        assertTrue(SlotRules.accepts(18, named));
        assertFalse(SlotRules.accepts(18, tnt));
        assertTrue(SlotRules.accepts(26, emerald));
        assertFalse(SlotRules.accepts(26, named));

        List<SlotRules.Item> inventory = new ArrayList<>(java.util.Collections.nCopies(27, null));
        inventory.set(18, new SlotRules.Item(SlotRules.ItemKind.OTHER, "playerone"));
        assertFalse(SlotRules.isBound(inventory, "PlayerOne"));
        inventory.set(25, named);
        assertTrue(SlotRules.isBound(inventory, "PlayerOne"));
        inventory.set(25, new SlotRules.Item(SlotRules.ItemKind.OTHER, null));
        assertFalse(SlotRules.isBound(inventory, "PlayerOne"));
    }

    @Test
    void duplicateMembershipIsRejectedAndBatteriesAreIsolatedByOwner() {
        UUID otherPlayer = UUID.fromString("2b77ef75-a4d9-40f8-83e4-2ac8c64f2d3a");
        Batteries batteries = new Batteries();
        assertTrue(batteries.add(PLAYER, "A", CANNON, 16));
        assertFalse(batteries.add(PLAYER, "A", CANNON, 16));
        assertTrue(batteries.add(otherPlayer, "A", CANNON, 16));
        assertEquals(Set.of(PLAYER, otherPlayer), batteries.owners(CANNON));
        assertFalse(batteries.remove(PLAYER, "A", new CannonId(
                "minecraft:overworld", 9, 64, 9), 16));
        assertEquals(Set.of(CANNON), batteries.members(PLAYER, "A"));
        assertEquals(Set.of(CANNON), batteries.members(otherPlayer, "A"));
    }

    @Test
    void returnedBatteryCollectionsAreSnapshotsAndCannotMutateState() {
        Batteries batteries = new Batteries();
        batteries.add(PLAYER, "A", CANNON, 16);
        Set<CannonId> members = batteries.members(PLAYER, "A");
        Map<String, Integer> listing = batteries.list(PLAYER);
        Map<UUID, Map<String, Set<CannonId>>> snapshot = batteries.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> members.add(
                new CannonId("minecraft:the_nether", 1, 2, 3)));
        assertThrows(UnsupportedOperationException.class, () -> listing.put("B", 1));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.get(PLAYER).put("B", Set.of(CANNON)));
        assertEquals(Map.of("A", 1), batteries.list(PLAYER));
    }

    @Test
    void everySlotBoundaryAndInvalidIndexMatchesTheCannonContract() {
        SlotRules.Item tnt = new SlotRules.Item(SlotRules.ItemKind.TNT, null);
        SlotRules.Item named = new SlotRules.Item(SlotRules.ItemKind.OTHER, "bound");
        SlotRules.Item emerald = new SlotRules.Item(SlotRules.ItemKind.EMERALD, null);
        for (int slot = 0; slot < 18; slot++) {
            assertTrue(SlotRules.accepts(slot, tnt), "TNT slot " + slot);
            assertFalse(SlotRules.accepts(slot, emerald), "emerald in ammo slot " + slot);
        }
        for (int slot = 18; slot < 26; slot++) {
            assertTrue(SlotRules.accepts(slot, named), "binding slot " + slot);
            assertFalse(SlotRules.accepts(slot, tnt), "TNT in binding slot " + slot);
        }
        assertTrue(SlotRules.accepts(26, emerald));
        assertFalse(SlotRules.accepts(26, named));
        assertFalse(SlotRules.accepts(-1, tnt));
        assertFalse(SlotRules.accepts(27, tnt));
        assertFalse(SlotRules.accepts(26, null));
    }

    @Test
    void bindingsOnlyReadSlotsEighteenThroughTwentyFiveAndShortInventoriesAreSafe() {
        List<SlotRules.Item> inventory = new ArrayList<>(java.util.Collections.nCopies(27, null));
        inventory.set(17, new SlotRules.Item(SlotRules.ItemKind.OTHER, "PlayerOne"));
        assertFalse(SlotRules.isBound(inventory, "PlayerOne"));
        inventory.set(25, new SlotRules.Item(SlotRules.ItemKind.OTHER, "PlayerOne"));
        assertTrue(SlotRules.isBound(inventory, "PlayerOne"));
        assertFalse(SlotRules.isBound(inventory.subList(0, 20), "PlayerOne"));
        assertFalse(SlotRules.isBound(inventory, "playerone"));
    }
}
