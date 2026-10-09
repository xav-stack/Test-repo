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
}
