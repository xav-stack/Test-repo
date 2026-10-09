package dev.cannons.mixin;

import dev.cannons.CannonItems;
import dev.cannons.CannonsMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
abstract class BlockDropsMixin {
    @Inject(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private static void cannons$customRegisteredDrop(BlockState state, ServerWorld world,
                                                      BlockPos pos, BlockEntity blockEntity,
                                                      CallbackInfoReturnable<List<ItemStack>> cir) {
        if (CannonsMod.runtime().shouldDropCannon(world, pos)) {
            cir.setReturnValue(List.of(CannonItems.create()));
        }
    }

    @Inject(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private static void cannons$customRegisteredDropWithEntity(BlockState state, ServerWorld world,
                                                               BlockPos pos, BlockEntity blockEntity,
                                                               Entity entity, ItemStack tool,
                                                               CallbackInfoReturnable<List<ItemStack>> cir) {
        if (entity instanceof PlayerEntity player && player.isCreative()
                && CannonsMod.runtime().shouldDropCannon(world, pos)) {
            cir.setReturnValue(List.of());
            return;
        }
        if (CannonsMod.runtime().shouldDropCannon(world, pos)) {
            cir.setReturnValue(List.of(CannonItems.create()));
        }
    }
}
