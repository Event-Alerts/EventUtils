package cc.aabss.eventutils;

import cc.aabss.eventutils.mixin.KeyBindingMixin;
import cc.aabss.eventutils.websocket.SocketEndpoint;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.text.Text.translatable;


public class KeybindManager {
    @NotNull private static final String CATEGORY = "key.category.eventutils";

    @Nullable private Long windowHandle;
    @NotNull public KeyBinding eventInfoKey;
    @NotNull private final Map<String, Long> lastKeyPresses = new HashMap<>();

    public KeybindManager(@NotNull EventUtils mod) {
        // Keybindings
        eventInfoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.eventinfo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY));
        // DEV: Uncomment to force test event
//        final KeyBindingMixin testEventKey = (KeyBindingMixin) KeyBindingHelper.registerKeyBinding(new KeyBinding(
//                "key.eventutils.testevent",
//                InputUtil.Type.KEYSYM,
//                GLFW.GLFW_KEY_SEMICOLON,
//                CATEGORY));
        final KeyBinding hidePlayersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.hideplayers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (windowHandle == null) windowHandle = client.getWindow().getHandle();

            // Event info key
            if (!eventInfoKey.isUnbound()) {
                if (GLFW.glfwGetKey(windowHandle, ((KeyBindingMixin) eventInfoKey).getBoundKey().getCode()) == GLFW.GLFW_PRESS) {
                    if (canNotPress(eventInfoKey)) return;
                    EventUtils.LOGGER.info("Event info key pressed");
                    if (SocketEndpoint.LAST_EVENT != null) {
                        client.setScreen(new EventInfoScreen(SocketEndpoint.LAST_EVENT));
                        return;
                    }
                    if (client.player != null) client.player.sendMessage(Text.translatable("eventutils.no_recent_event.message").formatted(Formatting.RED), true);
                }
            }

            // DEV: Uncomment to force test event
//            if (GLFW.glfwGetKey(WINDOW_HANDLE, testEventKey.getBoundKey().getCode()) == GLFW.GLFW_PRESS) {
//                if (canNotPress((KeyBinding) testEventKey)) return;
//                EventUtils.LOGGER.info("Test event key pressed");
//                mod.simulateTestEvent();
//                if (client.player == null) {
//                    EventUtils.LOGGER.info("Test event simulated from main menu");
//                    return;
//                }
//                client.player.sendMessage(Text.literal("Test event simulated! Check your server list and you should see a toast notification.").formatted(Formatting.GREEN), true);
//            }

            // In-game keybinds
            if (client.player == null) return;

            // Hide players key
            if (hidePlayersKey.wasPressed()) {
                mod.hidePlayers = !mod.hidePlayers;
                client.player.sendMessage(translatable(mod.hidePlayers ? "eventutils.hideplayers.enabled" : "eventutils.hideplayers.disabled")
                        .formatted(mod.hidePlayers ? Formatting.GREEN : Formatting.RED), true);
            }
        });
    }

    private boolean canNotPress(@NotNull KeyBinding keyBinding) {
        final String translationKey = keyBinding.getTranslationKey();
        final Long lastPressTime = lastKeyPresses.get(translationKey);
        final long now = System.currentTimeMillis();
        if (lastPressTime != null && now - lastPressTime < 500) return true;
        lastKeyPresses.put(translationKey, now);
        return false;
    }
}
