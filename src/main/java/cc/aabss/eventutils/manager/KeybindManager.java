package cc.aabss.eventutils.manager;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.mixin.KeyBindingAccessor;
import cc.aabss.eventutils.screen.EventInfoScreen;
import cc.aabss.eventutils.sdk.EventWrapper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
//? if >=1.21.11 {
/*import cc.aabss.eventutils.BuildProperties;
import net.minecraft.util.Identifier;
*///?}

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.text.Text.literal;
import static net.minecraft.text.Text.translatable;


public class KeybindManager {
    private static final long DEFAULT_COOLDOWN_TIME_MS = 500;
    //? if >=1.21.11 {
    /*@NotNull private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(BuildProperties.MOD_ID, BuildProperties.MOD_ID));
    *///? } else {
    @NotNull private static final String CATEGORY = "key.category.eventutils";
    //?}

    @Nullable private Long windowHandle;
    @NotNull public KeyBinding eventInfoKey;
    @NotNull private final Map<String, Long> lastKeyPresses = new HashMap<>();
    @Nullable public EventWrapper lastEventForInfoScreen;

    public KeybindManager(@NotNull EventUtils mod) {
        // Keybindings
        eventInfoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.eventinfo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                CATEGORY));
        final KeyBinding hidePlayersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.hideplayers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                CATEGORY));
        final KeyBinding testEventKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.testevent",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only allow if no screen or TitleScreen
            if (client.currentScreen != null && (!(client.currentScreen instanceof TitleScreen))) return;

            // Store Minecraft's window
            if (windowHandle == null) windowHandle = client.getWindow().getHandle();

            // Event info key
            if (!eventInfoKey.isUnbound()) {
                if (GLFW.glfwGetKey(windowHandle, ((KeyBindingAccessor) eventInfoKey).getBoundKey().getCode()) == GLFW.GLFW_PRESS) {
                    if (canNotPress(eventInfoKey, DEFAULT_COOLDOWN_TIME_MS)) return;
                    EventUtils.LOGGER.debug("Event info key pressed");

                    // If screen already open, close it
                    if (client.currentScreen instanceof EventInfoScreen) {
                        client.setScreen(null);
                        return;
                    }

                    // Open screen
                    if (lastEventForInfoScreen != null) {
                        client.setScreen(new EventInfoScreen(mod, lastEventForInfoScreen));
                        return;
                    }

                    // Action bar
                    if (client.player != null) client.player.sendMessage(Text.translatable("eventutils.no_recent_event.message").formatted(Formatting.RED), true);
                }
            }

            // Developer Mode: simulate test event
            if (!testEventKey.isUnbound() && mod.config.developer_mode) {
                if (GLFW.glfwGetKey(windowHandle, ((KeyBindingAccessor) testEventKey).getBoundKey().getCode()) == GLFW.GLFW_PRESS) {
                    if (canNotPress(testEventKey, DEFAULT_COOLDOWN_TIME_MS)) return;
                    EventUtils.LOGGER.debug("Test event key pressed");

                    // Simulate test event
                    mod.simulateTestEvent();
                    if (client.player == null) {
                        EventUtils.LOGGER.debug("Test event simulated from main menu");
                        return;
                    }

                    // Action bar
                    client.player.sendMessage(Text.literal("Test event simulated! Check your server list and you should see a toast notification.").formatted(Formatting.GREEN), true);
                }
            }

            // In-game keybinds
            if (client.player == null) return;

            // Hide players key
            if (hidePlayersKey.wasPressed()) {
                if (canNotPress(hidePlayersKey, 100)) return;
                EventUtils.LOGGER.debug("Hide players key pressed");

                // Cycle groups
                mod.groupManager.cycle();

                // Action bar
                final MutableText message;
                final Group group = mod.groupManager.getSelectedGroup();
                if (group != null) {
                    message = literal(group.getName());
                } else {
                    message = translatable("eventutils.hideplayers.view_revealed");
                }
                client.player.sendMessage(translatable("eventutils.hideplayers.view_prefix").append(message.formatted(Formatting.GREEN)), true);
            }
        });
    }

    private boolean canNotPress(@NotNull KeyBinding keyBinding, long cooldownTimeMs) {
        //? if >=1.21.11 {
        /*final String translationKey = keyBinding.getId();
        *///?} else {
        final String translationKey = keyBinding.getTranslationKey();
        //?}
        final Long lastPressTime = lastKeyPresses.get(translationKey);
        final long now = System.currentTimeMillis();
        if (lastPressTime != null && now - lastPressTime < cooldownTimeMs) return true;
        lastKeyPresses.put(translationKey, now);
        return false;
    }
}
