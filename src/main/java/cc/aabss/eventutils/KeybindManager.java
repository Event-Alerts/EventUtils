package cc.aabss.eventutils;

import cc.aabss.eventutils.config.PlayerGroup;
import cc.aabss.eventutils.mixin.KeyBindingMixin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
//? if >=1.21.11
/*import net.minecraft.util.Identifier;*/
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.text.Text.literal;
import static net.minecraft.text.Text.translatable;


public class KeybindManager {
    @NotNull private static final String CATEGORY = "key.category.eventutils";

    @Nullable private Long windowHandle;
    @NotNull public KeyBinding eventInfoKey;
    @NotNull private final Map<String, Long> lastKeyPresses = new HashMap<>();

    public KeybindManager(@NotNull EventUtils mod) {
        // Keybindings
        //? if >=1.21.11 {
        /*final KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(BuildProperties.MOD_ID, BuildProperties.MOD_ID));
        eventInfoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.eventinfo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                category));
        final KeyBinding hidePlayersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.hideplayers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                category));
        *///?} else {
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
        //?}
        // DEV: Uncomment to force test event
//        final KeyBindingMixin testEventKey = (KeyBindingMixin) KeyBindingHelper.registerKeyBinding(new KeyBinding(
//                "key.eventutils.testevent",
//                InputUtil.Type.KEYSYM,
//                GLFW.GLFW_KEY_SEMICOLON,
//                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (windowHandle == null) windowHandle = client.getWindow().getHandle();

            // Event info key TODO: prevent activating when in chat box or similar
            if (!eventInfoKey.isUnbound()) {
                if (GLFW.glfwGetKey(windowHandle, ((KeyBindingMixin) eventInfoKey).getBoundKey().getCode()) == GLFW.GLFW_PRESS) {
                    if (canNotPress(eventInfoKey)) return;
                    EventUtils.LOGGER.debug("Event info key pressed");
                    if (mod.lastEvent != null) {
                        client.setScreen(new EventInfoScreen(mod.lastEvent));
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

            // Hide players key: cycle Group 1 -> Group 2 -> ... -> Players Revealed -> repeat
            if (hidePlayersKey.wasPressed()) {
                final int groupsSize = mod.config.groups.size();
                if (mod.hidePlayersMode == HidePlayersMode.REVEALED) {
                    if (groupsSize == 0) {
                        // No groups, hide all players
                        mod.hidePlayersMode = HidePlayersMode.HIDE_ALL;
                    } else {
                        // Cycle to first group
                        mod.hidePlayersMode = HidePlayersMode.GROUP;
                        mod.selectedGroup = 0;
                    }
                } else if (mod.hidePlayersMode == HidePlayersMode.HIDE_ALL) {
                    mod.hidePlayersMode = HidePlayersMode.REVEALED;
                } else {
                    // Cycle to next group
                    mod.selectedGroup++;
                    // Reached end of groups, cycle back to players revealed
                    if (mod.selectedGroup >= groupsSize) mod.hidePlayersMode = HidePlayersMode.REVEALED;
                }

                // Send action bar
                final Text message;
                if (EventUtils.MOD.isHidePlayersRevealed()) {
                    message = translatable("eventutils.hideplayers.view_revealed").formatted(Formatting.GREEN);
                } else {
                    final PlayerGroup group = EventUtils.MOD.getCurrentViewGroup();
                    message = (group != null ? literal(group.getName()) : translatable("eventutils.hideplayers.view_whitelist_only")).formatted(Formatting.GREEN);
                }
                client.player.sendMessage(translatable("eventutils.hideplayers.view_prefix").append(message), true);
            }
        });
    }

    private boolean canNotPress(@NotNull KeyBinding keyBinding) {
        //? if >=1.21.11 {
        /*final String translationKey = keyBinding.getId();
        *///?} else {
        final String translationKey = keyBinding.getTranslationKey();
        //?}
        final Long lastPressTime = lastKeyPresses.get(translationKey);
        final long now = System.currentTimeMillis();
        if (lastPressTime != null && now - lastPressTime < 500) return true;
        lastKeyPresses.put(translationKey, now);
        return false;
    }
}
