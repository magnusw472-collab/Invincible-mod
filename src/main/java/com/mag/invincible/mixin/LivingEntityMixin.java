package com.mag.invincible.mixin;

import com.mag.invincible.InvincibleMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Clamps incoming damage BEFORE Minecraft's own damage/armor/totem pipeline
 * runs, so a player in Last Stand mode never actually reaches lethal health
 * in the first place. This sidesteps needing to hook the exact "about to
 * die" point, which has been renamed/restructured multiple times across
 * 1.21.x and turned out not to reliably prevent death even when it loaded
 * without errors (this version's death handling appears to commit to
 * killing the entity before the old die()/onDeath() hook actually runs).
 *
 * Trade-off: because the clamp happens before totem-of-undying logic runs,
 * a totem will NOT be consumed while Last Stand is what's actually saving
 * the player (the hit is no longer lethal by the time totem logic checks).
 * Totems still work completely normally the rest of the time - only when a
 * hit would otherwise be fatal while Last Stand is on does this apply.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float invincible$clampDamage(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayerEntity player)) {
            return amount;
        }
        if (!InvincibleMod.isLastStandEnabled(player)) {
            return amount;
        }

        float currentHealth = self.getHealth();
        if (currentHealth - amount <= 0f) {
            // Clamp so the hit leaves exactly 1.0 health (half a heart)
            // instead of going lethal.
            return Math.max(0f, currentHealth - 1.0f);
        }
        return amount;
    }
}
