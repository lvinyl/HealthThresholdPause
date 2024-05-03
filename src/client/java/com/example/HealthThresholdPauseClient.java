package com.example;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HealthThresholdPauseClient implements ClientModInitializer {

    private float healthThreshold = 5.0f; // Default threshold
    private float previousThreshold = 5.0f; // Stores previous threshold
    private boolean isPaused = false;
    private boolean isHealthThresholdEnabled = true; // New flag
    private KeyBinding toggleKey; // New KeyBinding

    @Override
    public void onInitializeClient() {
        // Initialize the KeyBinding
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.healththresholdpauseclient.toggle", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H, // H keycode
            "category.healththresholdpauseclient" // Keybinding category.
        ));

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
                            }))
                    .then(ClientCommandManager.literal("toggle") // Subcommand for toggle
                            .executes(context -> {
                                isHealthThresholdEnabled = !isHealthThresholdEnabled;
                                String message = (isHealthThresholdEnabled) ? "Health threshold enabled." : "Health threshold disabled.";
                                context.getSource().sendFeedback(Text.literal(message));
                                return 1;
                            }))
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null && isHealthThresholdEnabled) { // Check the flag
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

        // Check if keybind is pressed
        while (toggleKey.wasPressed()) {
            isHealthThresholdEnabled = !isHealthThresholdEnabled;
            client.inGameHud.getChatHud().addMessage(Text.literal((isHealthThresholdEnabled) ? "Health threshold enabled." : "Health threshold disabled.")); // Add toggle message
        }
    }
}
