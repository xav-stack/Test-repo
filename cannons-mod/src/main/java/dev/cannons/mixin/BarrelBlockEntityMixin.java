package dev.cannons.mixin;

import dev.cannons.CannonsMod;
import dev.cannons.screen.CannonScreenHandler;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BarrelBlockEntity.class)
abstract class BarrelBlockEntityMixin {
    @Inject(method = "createScreenHandler", at = @At("HEAD"), cancellable = true)
    private void cannons$openCustomScreen(int syncId, PlayerInventory playerInventory,
                                          CallbackInfoReturnable<ScreenHandler> cir) {
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        World world = barrel.getWorld();
        if (world instanceof ServerWorld serverWorld
                && CannonsMod.runtime().isRegistered(serverWorld, barrel.getPos())) {
            cir.setReturnValue(new CannonScreenHandler(
                    syncId, playerInventory, (Inventory) (Object) barrel));
        }
    }

    public boolean isValid(int slot, ItemStack stack) {
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        World world = barrel.getWorld();
        if (!(world instanceof ServerWorld serverWorld)
                || !CannonsMod.runtime().isRegistered(serverWorld, barrel.getPos())) {
            return true;
        }
        return slot >= 0 && slot < 18 && stack.isOf(Items.TNT);
    }
}
