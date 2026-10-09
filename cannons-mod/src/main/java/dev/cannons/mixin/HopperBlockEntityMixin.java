package dev.cannons.mixin;

import dev.cannons.CannonsMod;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.IntStream;

@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
    @Inject(method = "getAvailableSlots", at = @At("HEAD"), cancellable = true)
    private static void cannons$limitAutomation(Inventory inventory, Direction side,
                                                CallbackInfoReturnable<IntStream> cir) {
        if (inventory instanceof BlockEntity blockEntity
                && blockEntity.getWorld() instanceof ServerWorld serverWorld
                && CannonsMod.runtime().isRegistered(serverWorld, blockEntity.getPos())) {
            cir.setReturnValue(IntStream.range(0, Math.min(18, inventory.size())));
        }
    }
}
