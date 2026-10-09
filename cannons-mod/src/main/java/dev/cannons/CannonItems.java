package dev.cannons;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class CannonItems {
    public static final String ITEM_TAG = "CannonItem";

    private CannonItems() {
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.BARREL);
        stack.setCustomName(Text.literal("Cannon")
                .setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(false)));
        stack.addEnchantment(Enchantments.UNBREAKING, 1);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putByte(ITEM_TAG, (byte) 1);
        nbt.putInt("HideFlags", 1);
        return stack;
    }

    public static boolean isCannonItem(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isOf(Items.BARREL)
                && stack.getNbt() != null
                && stack.getNbt().getByte(ITEM_TAG) == 1;
    }
}
