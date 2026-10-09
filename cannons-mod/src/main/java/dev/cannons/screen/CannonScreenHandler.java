package dev.cannons.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public final class CannonScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public CannonScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        checkSize(inventory, 27);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9;
                addSlot(new CannonSlot(inventory, index, 8 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.screen.slot.Slot(
                        playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.screen.slot.Slot(playerInventory, column,
                    8 + column * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return moved;
        }
        var slot = slots.get(slotIndex);
        if (!slot.hasStack()) {
            return moved;
        }
        ItemStack stack = slot.getStack();
        moved = stack.copy();
        if (slotIndex < 27) {
            if (!insertItem(stack, 27, 63, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean inserted = false;
            if (stack.isOf(Items.TNT)) {
                inserted = insertItem(stack, 0, 18, false);
            }
            if (stack.hasCustomName() && !stack.isEmpty()) {
                inserted = insertItem(stack, 18, 26, false) || inserted;
            }
            if (stack.isOf(Items.EMERALD) && !stack.isEmpty()) {
                inserted = insertItem(stack, 26, 27, false) || inserted;
            }
            if (!stack.isOf(Items.TNT) && !stack.hasCustomName() && !stack.isOf(Items.EMERALD)) {
                return ItemStack.EMPTY;
            }
            if (!inserted) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTakeItem(player, stack);
        return moved;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }
}
