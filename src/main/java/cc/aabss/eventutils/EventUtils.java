package cc.aabss.eventutils;

import cc.aabss.eventutils.websocket.SocketEndpoint;
import cc.aabss.eventutils.websocket.WebSocketClient;
import cc.aabss.eventutils.config.EventConfig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static net.minecraft.text.Text.translatable;


public class EventUtils implements ClientModInitializer {
    /**
     * Only use if it is absolutely impossible to access the mod instance through other (safer) means
     * <br>This is usually only necessary for mixins!
     */
    public static EventUtils MOD;
    @NotNull public static final Logger LOGGER = LogManager.getLogger(EventUtils.class, new PrefixMessageFactory());
    @NotNull public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(3);
    @NotNull public static final String QUEUE_TEXT = "\n\n Per-server ranks get a higher priority in their respective queues. To receive such a rank, purchase one at\n store.invadedlands.net.\n\nTo leave a queue, use the command: /leavequeue.\n";

    @NotNull public final EventConfig config = new EventConfig();
    @NotNull public final UpdateChecker updateChecker = new UpdateChecker(this);
    @NotNull public final DiscordRPC discordRPC = new DiscordRPC(this);
    @NotNull public final Map<EventType, String> lastIps = new EnumMap<>(EventType.class);
    public boolean hidePlayers = false;

    public EventUtils() {
        MOD = this;
    }

    @Override
    public void onInitializeClient() {
        discordRPC.connect();
        updateChecker.checkUpdate();

        // Websockets
        final Set<WebSocketClient> webSockets = new HashSet<>();
        webSockets.add(new WebSocketClient(this, SocketEndpoint.EVENT_POSTED));
        webSockets.add(new WebSocketClient(this, SocketEndpoint.FAMOUS_EVENT));
        webSockets.add(new WebSocketClient(this, SocketEndpoint.POTENTIAL_FAMOUS_EVENT));

        // Command registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> CommandRegister.register(dispatcher));

        // Game closed
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            webSockets.forEach(socket -> socket.close("Game closed"));
            discordRPC.disconnect();
        });

        // Update check notifier
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> updateChecker.notifyUpdate());

        // Hide players keybind
        final KeyBinding hidePlayersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.hideplayers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                "key.category.eventutils"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hidePlayersKey.wasPressed()) {
                hidePlayers = !hidePlayers;
                if (client.player != null) client.player.sendMessage(
                        translatable(hidePlayers ? "eventutils.hideplayers.enabled" : "eventutils.hideplayers.disabled")
                                .formatted(hidePlayers ? Formatting.GREEN : Formatting.RED), true);
            }
        });

        // Simple queue message
        ClientReceiveMessageEvents.ALLOW_GAME.register(((text, overlay) -> true));
        ClientReceiveMessageEvents.MODIFY_GAME.register(((text, overlay) -> {
            if (!config.simpleQueueMessage) return text;
            final String original = text.getString();
            if (!original.contains(QUEUE_TEXT)) return text;
            final MutableText resultText = Text.literal("");
            for (final String line : original.replace(QUEUE_TEXT, "").replaceFirst("\n", "").split("\n")) {
                final String[] parts = line.split(": ");
                if (parts.length <= 1) {
                    resultText.append(Text.literal(line).formatted(Formatting.GOLD));
                    continue;
                }
                if (!resultText.getSiblings().isEmpty()) resultText.append("\n");
                final String[] valueParts = parts[1].split("/");
                resultText.append(Text.literal(parts[0]).formatted(Formatting.GOLD).append(": ")
                        .append(Text.literal(valueParts[0]).formatted(Formatting.YELLOW))
                        .append(Text.literal("/").formatted(Formatting.GOLD))
                        .append(Text.literal(valueParts[1]).formatted(Formatting.YELLOW)));
            }
            return resultText;
        }));
    }

    @Nullable
    public String getIpAndConnect(@NotNull EventType eventType, @NotNull Object message) {
        // Check if Famous/Potential Famous
        if (message instanceof String messageString) {
            final String ip = ConnectUtility.getIp(messageString);
            if (config.autoTp) ConnectUtility.connect(ip == null ? config.defaultFamousIp : ip);
            return ip;
        }
        if (!(message instanceof JsonObject messageJson)) return null;

        // Get IP
        String ip = "hypixel.net";
        if (eventType != EventType.HOUSING) {
            final JsonElement messageIp = messageJson.get("ip");
            if (messageIp != null) { // Specifically provided
                ip = messageIp.getAsString();
            } else { // Extract from description
                final JsonElement messageDescription = messageJson.get("description");
                if (messageDescription != null) ip = ConnectUtility.getIp(messageDescription.getAsString());
            }
        }

        // Auto TP if enabled
        if (config.autoTp && ip != null) ConnectUtility.connect(ip);
        return ip;
    }

    @NotNull
    public static String translate(@NotNull String key) {
        return Language.getInstance().get(key);
    }
}
