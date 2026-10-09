package dev.cannons.screen;

import dev.cannons.pure.logic.SlotRules;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

public final class CannonSlot extends Slot {
    private final int cannonSlot;

    public CannonSlot(Inventory inventory, int cannonSlot, int x, int y) {
        super(inventory, cannonSlot, x, y);
        this.cannonSlot = cannonSlot;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        SlotRules.ItemKind kind = stack.isOf(Items.TNT)
                ? SlotRules.ItemKind.TNT
                : stack.isOf(Items.EMERALD) ? SlotRules.ItemKind.EMERALD : SlotRules.ItemKind.OTHER;
        String name = stack.hasCustomName() ? stack.getName().getString() : null;
        return SlotRules.accepts(cannonSlot, new SlotRules.Item(kind, name));
    }
}
