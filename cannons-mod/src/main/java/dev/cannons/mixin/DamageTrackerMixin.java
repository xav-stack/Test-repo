package dev.cannons.mixin;

import dev.cannons.runtime.CannonsRuntime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DamageTracker.class)
abstract class DamageTrackerMixin {
    @Shadow @Final private List<DamageRecord> recentDamage;
    @Shadow @Final private LivingEntity entity;

    @Inject(method = "getDeathMessage", at = @At("RETURN"), cancellable = true)
    private void cannons$ballisticDeathMessage(CallbackInfoReturnable<Text> cir) {
        if (!(entity.getWorld() instanceof ServerWorld serverWorld) || recentDamage.isEmpty()) {
            return;
        }
        DamageSource source = recentDamage.get(recentDamage.size() - 1).damageSource();
        Entity projectile = source.getSource();
        if (!(projectile instanceof TntEntity)
                || !projectile.getCommandTags().contains(CannonsRuntime.PROJECTILE_TAG)) {
            return;
        }
        String[] messages = {
                " experienced ballistics.",
                " was hammered.",
                " failed to look up."
        };
        cir.setReturnValue(Text.literal(entity.getDisplayName().getString()
                + messages[serverWorld.random.nextInt(messages.length)]));
    }
}
