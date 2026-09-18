package com.mag.invincible;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InvincibleMod implements ModInitializer {

    // In-memory only: resets on server restart/reload.
    private static final Set<UUID> LAST_STAND_PLAYERS = Collections.synchronizedSet(new HashSet<>());

    public static boolean isLastStandEnabled(ServerPlayerEntity player) {
        return LAST_STAND_PLAYERS.contains(player.getUuid());
    }

    /**
     * 1.21.11 replaced the old int-level ServerCommandSource#hasPermissionLevel(int)
     * check with a new Permission/PermissionPredicate system. Rather than depend on
     * that brand-new, still-shifting API, this checks operator status directly via
     * PlayerManager#isOperator(GameProfile) - the underlying ops.json check that has
     * been stable across every Minecraft version and isn't part of that rewrite.
     * Non-player sources (console/command blocks) are allowed, matching the old
     * "level 2+" behavior for them.
     */
private static boolean isOperator(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return true;
        }
        return source.getServer().getPlayerManager()
                .isOperator(new net.minecraft.server.PlayerConfigEntry(player.getGameProfile()));
    }
    private static void toggle(ServerPlayerEntity target, ServerPlayerEntity invoker) {
        UUID id = target.getUuid();
        boolean nowEnabled;

        if (LAST_STAND_PLAYERS.contains(id)) {
            LAST_STAND_PLAYERS.remove(id);
            nowEnabled = false;
        } else {
            LAST_STAND_PLAYERS.add(id);
            nowEnabled = true;
        }

        target.sendMessage(
                Text.literal(nowEnabled
                        ? "Last Stand enabled - a lethal hit will leave you at half a heart instead of killing you."
                        : "Last Stand disabled - you can die normally again."),
                false
        );

        if (invoker != null && invoker != target) {
            invoker.sendMessage(
                    Text.literal((nowEnabled ? "Enabled" : "Disabled") + " Last Stand for "
                            + target.getName().getString() + "."),
                    false
            );
        }
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            LiteralCommandNode<ServerCommandSource> invincibleNode = dispatcher.register(
                    CommandManager.literal("invincible")
                            .requires(InvincibleMod::isOperator)
                            .executes(context -> {
                                ServerPlayerEntity self = context.getSource().getPlayerOrThrow();
                                toggle(self, self);
                                return 1;
                            })
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .requires(InvincibleMod::isOperator)
                                    .executes(context -> {
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                        ServerPlayerEntity invoker = context.getSource().getPlayer();
                                        toggle(target, invoker);
                                        return 1;
                                    }))
            );

            // /god as a plain alias for /invincible
            dispatcher.register(
                    CommandManager.literal("god")
                            .requires(InvincibleMod::isOperator)
                            .redirect(invincibleNode)
            );
        });
    }
}
