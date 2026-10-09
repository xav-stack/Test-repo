package dev.cannons.mixin;

import dev.cannons.runtime.CannonsRuntime;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntEntity.class)
abstract class TntEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cannons$cancelVanillaProjectileTick(CallbackInfo ci) {
        TntEntity tnt = (TntEntity) (Object) this;
        if (tnt.getWorld() instanceof ServerWorld
                && tnt.getCommandTags().contains(CannonsRuntime.PROJECTILE_TAG)) {
            ci.cancel();
        }
    }
}
