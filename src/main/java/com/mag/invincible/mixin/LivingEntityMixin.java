package com.mag.invincible.mixin;

import com.mag.invincible.InvincibleMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Clamps incoming damage BEFORE Minecraft's own damage/armor/totem pipeline
 * runs, so a player in Last Stand mode never actually reaches lethal health
 * unless they have a death-protection item (a totem, or anything else using
 * DataComponentTypes.DEATH_PROTECTION) - in which case the hit is left
 * completely untouched so vanilla's own totem logic runs and consumes it
 * exactly as normal. Only when there's no such item does the clamp kick in.
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
            if (hasDeathProtection(player)) {
                // Let it through untouched - vanilla's totem check will
                // trigger normally and consume the totem as usual.
                return amount;
            }
            // No totem available: clamp so the hit leaves exactly 1.0
            // health (half a heart) instead of going lethal.
            return Math.max(0f, currentHealth - 1.0f);
        }
        return amount;
    }

    private static boolean hasDeathProtection(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        return mainHand.contains(DataComponentTypes.DEATH_PROTECTION)
                || offHand.contains(DataComponentTypes.DEATH_PROTECTION);
    }
}
