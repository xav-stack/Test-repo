package dev.cannons.mixin;

import dev.cannons.runtime.CannonsRuntime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "shouldSave", at = @At("HEAD"), cancellable = true)
    private void cannons$doNotSaveProjectiles(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TntEntity
                && entity.getWorld() instanceof ServerWorld
                && entity.getCommandTags().contains(CannonsRuntime.PROJECTILE_TAG)) {
            cir.setReturnValue(false);
        }
    }
}
