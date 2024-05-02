package com.example;

import com.mojang.brigadier.arguments.FloatArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class HealthThresholdPauseClient implements ClientModInitializer {

    private float healthThreshold = 5.0f; // Default threshold
    private float previousThreshold = 5.0f; // Stores previous threshold
    private boolean isPaused = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("healthThreshold")
                    .then(ClientCommandManager.literal("set") // Subcommand for set
                            .then(ClientCommandManager.argument("threshold", FloatArgumentType.floatArg())
                                    .executes(context -> {
                                        previousThreshold = healthThreshold; // Store previous value
                                        healthThreshold = context.getArgument("threshold", float.class);
                                        context.getSource().sendFeedback(Text.literal("Health threshold set to " + healthThreshold + " (Previous: " + previousThreshold + ")"));
                                        return 0;
                                    })))
                    .then(ClientCommandManager.literal("current") // Subcommand for current
                            .executes(context -> {
                                context.getSource().sendFeedback(Text.literal("Current health threshold: " + healthThreshold));
                                return 0;
                            })));
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            if (player.getHealth() <= healthThreshold && !isPaused) {
                if (client.isInSingleplayer()) {
                    client.openGameMenu(true);
                } else {
                    client.world.disconnect();
                }
                isPaused = true;
            } else if (player.getHealth() > healthThreshold && isPaused) {
                isPaused = false;
            }
        }
    }
}
