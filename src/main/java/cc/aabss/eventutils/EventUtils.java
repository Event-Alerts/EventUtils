package cc.aabss.eventutils;

import cc.aabss.eventutils.commands.CommandRegister;
import cc.aabss.eventutils.utility.ConnectUtility;
import cc.aabss.eventutils.websocket.SocketEndpoint;
import cc.aabss.eventutils.websocket.WebSocketClient;
import cc.aabss.eventutils.config.EventConfig;
import cc.aabss.eventutils.config.PlayerGroup;
import cc.aabss.eventutils.plustag.EventAlertsApi;

import com.google.gson.JsonObject;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class EventUtils implements ClientModInitializer {
    /**
     * Only use if it is absolutely impossible to access the mod instance through other (safer) means
     * <br>This is usually only necessary for mixins!
     */
    public static EventUtils MOD;
    @NotNull public static final Logger LOGGER = LogManager.getLogger(EventUtils.class, new PrefixMessageFactory());
    @NotNull public static final String QUEUE_TEXT = "\n\n Per-server ranks get a higher priority in their respective queues. To receive such a rank, purchase one at\n store.invadedlands.net.\n\nTo leave a queue, use the command: /leavequeue.\n";
    @NotNull public static final MutableText MESSAGE_PREFIX = Text.literal("EventUtils")
            .formatted(Formatting.BOLD)
            .fillStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF5AA42)))
            .append(Text.literal("§r »")
                    .fillStyle(Style.EMPTY.withBold(false).withColor(TextColor.fromRgb(0xB57C2F))));
    @NotNull public static final MutableText ERROR_MESSAGE_PREFIX = Text.literal("EventUtils")
            .formatted(Formatting.BOLD)
            .formatted(Formatting.RED)
            .append(Text.literal("§r§4 »")
                    .fillStyle(Style.EMPTY.withBold(false)));

    @NotNull public final EventConfig config = new EventConfig();
    @NotNull public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    @NotNull public final Set<WebSocketClient> webSockets = new HashSet<>();
    @NotNull public final UpdateChecker updateChecker = new UpdateChecker(this);
    public KeybindManager keybindManager;
    @NotNull public final EventServerManager eventServerManager = new EventServerManager(this);
    @NotNull public final Map<EventType, String> lastIps = new EnumMap<>(EventType.class);
    /** 0 = first group (or hide-all when no groups), 1 = second group, ... ; groups.size() = players revealed */
    public int hidePlayersViewMode = 0;

    public EventUtils() {
        MOD = this;
    }

    @Override
    public void onInitializeClient() {

        // Websockets
        webSockets.add(new WebSocketClient(this, SocketEndpoint.EVENT_POSTED));
        webSockets.add(new WebSocketClient(this, SocketEndpoint.FAMOUS_EVENT_POSTED));
        webSockets.add(new WebSocketClient(this, SocketEndpoint.EVENT_CANCELLED));

        // Command registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> CommandRegister.register(dispatcher));

        // Game closed
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            webSockets.forEach(socket -> socket.close("Game closed"));
            eventServerManager.removeAllEventServers();
        });

        // Update checker
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> updateChecker.checkUpdate());

        // Fetch Event Alerts plus tags for local player
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                var uuid = client.player.getUuid();
                LOGGER.info("[EventUtils] JOIN: scheduling Event Alerts fetch for local player uuid={}", uuid);
                EventAlertsApi.scheduleFetchIfNeeded(uuid.toString());
            } else {
                LOGGER.info("[EventUtils] JOIN: client.player is null, skipping fetch (will retry when tab list is opened)");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("[EventUtils] DISCONNECT: clearing Event Alerts cache");
            EventAlertsApi.clearCache();
        });

        // Initialize keybind manager
        keybindManager = new KeybindManager(this);

        // Simple queue message
        ClientReceiveMessageEvents.ALLOW_GAME.register(((text, overlay) -> true));
        ClientReceiveMessageEvents.MODIFY_GAME.register(((text, overlay) -> {
            if (config.simpleQueueMessage && text.getString().contains(QUEUE_TEXT)) {
                final String original = text.getString();
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
            }
            // May need to manipulate later
            return text;
        }));
    }

    @Nullable
    public String getIpAndConnect(@NotNull EventType eventType, @NotNull JsonObject message) {
        // Check if Famous/Potential Famous/Sighting
        if (eventType == EventType.SKEPPY || eventType == EventType.POTENTIAL_FAMOUS || eventType == EventType.SIGHTING || eventType == EventType.FAMOUS) {
            final String ip = ConnectUtility.getIp(message.get("message").getAsString());
            if (config.autoTp) ConnectUtility.connect(ip == null ? config.defaultFamousIp : ip);
            return ip;
        }

        // Get IP
        String ip = null;
        if (eventType == EventType.HOUSING) {
            ip = "hypixel.net";
        } else {
            final String extracted = ConnectUtility.extractIp(message);
            if (extracted != null && !extracted.isEmpty()) ip = extracted;
        }

        // Auto TP if enabled
        if (config.autoTp && ip != null) ConnectUtility.connect(ip);

        return ip;
    }

    public static boolean isNPC(@NotNull String name, boolean bypass) {
        return (MOD.config.hideNPCs || bypass) && looksLikeNPC(name);
    }

    public static boolean isNPC(@NotNull String name) {
        return isNPC(name, false);
    }

    public static boolean looksLikeNPC(@NotNull String name) {
        return name.contains("[") || name.contains("]") || name.contains(" ") || name.contains("-") || name.equals("§z");
    }

    /** Whether the current view mode is "players revealed" (show everyone). */
    public boolean isHidePlayersRevealed() {
        final int n = config.groups.size();
        if (n == 0) return hidePlayersViewMode == 1;
        return hidePlayersViewMode >= n;
    }

    /** Whether we are in a "hide" mode (any group or hide-all). */
    public boolean isInHidePlayersMode() {
        final int n = config.groups.size();
        if (n == 0) return hidePlayersViewMode == 0;
        return hidePlayersViewMode < n;
    }

    /** Current group when in group view mode, or null if revealed or no groups. */
    @Nullable
    public PlayerGroup getCurrentViewGroup() {
        final var groups = config.groups;
        if (groups.isEmpty() || hidePlayersViewMode >= groups.size()) return null;
        return groups.get(hidePlayersViewMode);
    }

    /**
     * True if the player (by lowercased name) should be visible with current view mode.
     * Caller must exclude main player.
     */
    public boolean isPlayerVisible(@NotNull String nameLower) {
        if (isHidePlayersRevealed()) return true;
        if (config.whitelistedPlayers.contains(nameLower)) return true;
        final PlayerGroup group = getCurrentViewGroup();
        final boolean isNpc = looksLikeNPC(nameLower);

        // NPC behavior: if the global hide toggle is OFF, NPCs should always stay visible.
        if (isNpc) {
            if (!config.hideNPCs) return true;
            if (group == null) return false;
            final boolean listed = group.containsPlayer(nameLower);
            return group.isHideListedNpcs() ? !listed : listed;
        }

        if (group == null) return false; // no groups, hide mode: only whitelisted players are visible
        final boolean listed = group.containsPlayer(nameLower);
        return group.isHideListedPlayers() ? !listed : listed;
    }

    /** True if the nametag for this visible player should be drawn (per-group setting when in group view). */
    public boolean shouldShowNametagFor(@NotNull String nameLower) {
        if (!isInHidePlayersMode()) return true;
        final PlayerGroup group = getCurrentViewGroup();
        if (group == null) return true; // hide-all with no groups: use default
        if (!group.containsPlayer(nameLower)) return true; // whitelist/NPC visibility: show nametag
        return group.isShowNametags();
    }

    @Contract(pure = true)
    public static int max(int... values) {
        int max = Integer.MIN_VALUE;
        for (final int value : values) if (value > max) max = value;
        return max;
    }

    @NotNull
    public static String translate(@NotNull String key) {
        return Language.getInstance().get(key);
    }

    /**
     * Simulates an event being posted for testing purposes
     */
    public void simulateTestEvent() {
        final long currentTime = System.currentTimeMillis();
        final long eventTime = currentTime + (30 * 1000);

        // Create a test event JSON object with the correct structure
        final JsonObject testEvent = new JsonObject();
        testEvent.addProperty("id", "test-event-" + currentTime);
        testEvent.addProperty("title", "Test Event");
        testEvent.addProperty("description", "This is a simulated test event for testing the server list feature. Server: mc.hypixel.net");
        testEvent.addProperty("time", eventTime);
        testEvent.addProperty("ip", "invadedlands.net");
        testEvent.addProperty("prize", "$1000");

        // Add the rolesNamed array that EventType.fromJson expects
        final com.google.gson.JsonArray rolesArray = new com.google.gson.JsonArray();
        rolesArray.add("MONEY");
        testEvent.add("rolesNamed", rolesArray);

        LOGGER.info("Simulating test event: {}", testEvent.toString());

        // Process the event through the EVENT_POSTED handler
        SocketEndpoint.EVENT_POSTED.handler.accept(this, testEvent.toString());

        // Set as last event for event info screen
        SocketEndpoint.LAST_EVENT = testEvent;
    }
}
