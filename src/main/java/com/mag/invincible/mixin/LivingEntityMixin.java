package com.mag.invincible.mixin;

import com.mag.invincible.InvincibleMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * By the time LivingEntity#die(DamageSource) is called, vanilla has already:
 *  - applied armor / potion / enchantment damage modifiers,
 *  - reduced health accordingly,
 *  - checked for and consumed a Totem of Undying if one would save the entity.
 *
 * die() only runs if none of that saved them and health is at/below 0.
 * That makes it the right, minimally-invasive place to intervene: totems and
 * normal damage are completely untouched, we only step in for the literal
 * "about to die" moment.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void invincible$preventDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!InvincibleMod.isLastStandEnabled(player)) {
            return;
        }

        ci.cancel();
        self.setHealth(1.0f); // half a heart
        self.setFireTicks(0); // avoid immediately re-triggering from lingering fire
    }
}
