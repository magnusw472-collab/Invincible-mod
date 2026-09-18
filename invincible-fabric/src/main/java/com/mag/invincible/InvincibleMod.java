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
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> {
                                ServerPlayerEntity self = context.getSource().getPlayerOrThrow();
                                toggle(self, self);
                                return 1;
                            })
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .requires(source -> source.hasPermissionLevel(2))
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
                            .requires(source -> source.hasPermissionLevel(2))
                            .redirect(invincibleNode)
            );
        });
    }
}
