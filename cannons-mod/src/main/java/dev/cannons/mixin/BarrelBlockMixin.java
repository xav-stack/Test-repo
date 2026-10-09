package dev.cannons.mixin;

import dev.cannons.CannonItems;
import dev.cannons.CannonsMod;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BarrelBlock.class)
abstract class BarrelBlockMixin {
    @Inject(method = "onPlaced", at = @At("TAIL"))
    private void cannons$registerOnPlace(World world, BlockPos pos, BlockState state,
                                         LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && CannonItems.isCannonItem(stack)) {
            CannonsMod.runtime().onCannonItemPlaced(serverWorld, pos);
        }
    }

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void cannons$unregisterOnReplacement(BlockState state, World world, BlockPos pos,
                                                  BlockState newState, boolean moved, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld
                && !newState.isOf(Blocks.BARREL)
                && CannonsMod.runtime().isRegistered(serverWorld, pos)) {
            CannonsMod.runtime().unregisterCannon(serverWorld, pos);
        }
    }
}
