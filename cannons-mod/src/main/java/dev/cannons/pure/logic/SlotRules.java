package dev.cannons.pure.logic;

import java.util.List;

public final class SlotRules {
    public enum ItemKind {
        TNT,
        EMERALD,
        OTHER
    }

    public record Item(ItemKind kind, String customName) {
    }

    private SlotRules() {
    }

    public static boolean accepts(int slot, Item item) {
        if (item == null || slot < 0 || slot >= 27) {
            return false;
        }
        if (slot < 18) {
            return item.kind() == ItemKind.TNT;
        }
        if (slot < 26) {
            return item.customName() != null;
        }
        return item.kind() == ItemKind.EMERALD;
    }

    public static boolean isBound(List<Item> inventory, String username) {
        for (int slot = 18; slot <= 25 && slot < inventory.size(); slot++) {
            Item item = inventory.get(slot);
            if (item != null && NameBinding.matches(item.customName(), username)) {
                return true;
            }
        }
        return false;
    }
}
