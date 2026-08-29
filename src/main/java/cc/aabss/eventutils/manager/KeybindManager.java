package cc.aabss.eventutils.manager;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.mixin.KeyMappingAccessor;
import cc.aabss.eventutils.screen.EventInfoScreen;
import cc.aabss.eventutils.sdk.EventWrapper;
import cc.aabss.eventutils.versioning.VersionedClient;
import cc.aabss.eventutils.versioning.VersionedLocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
//? if >= 26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}
//? if >=1.21.11 {
/*import cc.aabss.eventutils.BuildProperties;
import net.minecraft.resources.Identifier;
*///?}

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;


public class KeybindManager {
    private static final long DEFAULT_COOLDOWN_TIME_MS = 500;
    //? if >=1.21.11 {
    /*@NotNull private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BuildProperties.MOD_ID, BuildProperties.MOD_ID));
    *///? } else {
    @NotNull private static final String CATEGORY = "key.category.eventutils";
    //?}

    @Nullable private Long windowHandle;
    @NotNull public KeyMapping eventInfoKey;
    @NotNull private final Map<String, Long> lastKeyPresses = new HashMap<>();
    @Nullable public EventWrapper lastEventForInfoScreen;

    public KeybindManager(@NotNull EventUtils mod) {
        // Keybindings
        eventInfoKey = register(new KeyMapping(
                "key.eventutils.eventinfo",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                CATEGORY));
        final KeyMapping hidePlayersKey = register(new KeyMapping(
                "key.eventutils.hideplayers",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                CATEGORY));
        final KeyMapping testEventKey = register(new KeyMapping(
                "key.eventutils.testevent",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            final VersionedClient vClient = new VersionedClient(client);

            // Only allow if no screen or TitleScreen
            if (vClient.screen() != null && (!(vClient.screen() instanceof TitleScreen))) return;

            // Store Minecraft's window
            if (windowHandle == null) {
                //? if >=1.21.11 {
                /*windowHandle = client.getWindow().handle();
                *///?} else {
                windowHandle = client.getWindow().getWindow();
                //?}
            }

            // Event info key
            if (!eventInfoKey.isUnbound()) {
                if (GLFW.glfwGetKey(windowHandle, ((KeyMappingAccessor) eventInfoKey).getKey().getValue()) == GLFW.GLFW_PRESS) {
                    if (canNotPress(eventInfoKey, DEFAULT_COOLDOWN_TIME_MS)) return;
                    EventUtils.LOGGER.debug("Event info key pressed");

                    // If screen already open, close it
                    if (vClient.screen() instanceof EventInfoScreen) {
                        vClient.setScreen(null);
                        return;
                    }

                    // Open screen
                    if (lastEventForInfoScreen != null) {
                        vClient.setScreen(new EventInfoScreen(mod, lastEventForInfoScreen));
                        return;
                    }

                    // Action bar
                    if (client.player != null) {
                        new VersionedLocalPlayer(client.player).sendActionBar(translatable("eventutils.no_recent_event.message").withStyle(ChatFormatting.RED));
                    }
                }
            }

            // Developer Mode: simulate test event
            if (!testEventKey.isUnbound() && mod.config.developer_mode) {
                if (GLFW.glfwGetKey(windowHandle, ((KeyMappingAccessor) testEventKey).getKey().getValue()) == GLFW.GLFW_PRESS) {
                    if (canNotPress(testEventKey, DEFAULT_COOLDOWN_TIME_MS)) return;
                    EventUtils.LOGGER.debug("Test event key pressed");

                    // Simulate test event
                    mod.simulateTestEvent();
                    if (client.player == null) {
                        EventUtils.LOGGER.debug("Test event simulated from main menu");
                        return;
                    }

                    // Action bar
                    new VersionedLocalPlayer(client.player).sendActionBar(literal("Test event simulated! Check your server list and you should see a toast notification.").withStyle(ChatFormatting.GREEN));
                }
            }

            // In-game keybinds
            if (client.player == null) return;

            // Hide players key
            if (hidePlayersKey.consumeClick()) {
                if (canNotPress(hidePlayersKey, 100)) return;
                EventUtils.LOGGER.debug("Hide players key pressed");

                // Cycle groups
                mod.groupManager.cycle();

                // Action bar
                final MutableComponent message;
                final Group group = mod.groupManager.getSelectedGroup();
                if (group != null) {
                    message = literal(group.getName());
                } else {
                    message = translatable("eventutils.hideplayers.view_revealed");
                }
                new VersionedLocalPlayer(client.player).sendActionBar(translatable("eventutils.hideplayers.view_prefix").append(message.withStyle(ChatFormatting.GREEN)));
            }
        });
    }

    @NotNull
    private KeyMapping register(@NotNull KeyMapping keyMapping) {
        //? if >=26.1 {
        /*return KeyMappingHelper.registerKeyMapping(keyMapping);
        *///?} else {
        return KeyBindingHelper.registerKeyBinding(keyMapping);
        //?}
    }

    private boolean canNotPress(@NotNull KeyMapping keyBinding, long cooldownTimeMs) {
        final String name = keyBinding.getName();
        final Long lastPressTime = lastKeyPresses.get(name);
        final long now = System.currentTimeMillis();
        if (lastPressTime != null && now - lastPressTime < cooldownTimeMs) return true;
        lastKeyPresses.put(name, now);
        return false;
    }
}
