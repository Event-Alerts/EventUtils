package cc.aabss.eventutils;

import cc.aabss.eventutils.commands.CommandRegister;
import cc.aabss.eventutils.config.EventConfig;
import cc.aabss.eventutils.config.PlayerGroup;
import cc.aabss.eventutils.plustag.EventAlertsApi;
import cc.aabss.eventutils.websocket.listener.EventCancelledListener;
import cc.aabss.eventutils.websocket.listener.EventPostedListener;
import cc.aabss.eventutils.websocket.listener.FamousEventPostedListener;
import com.mojang.authlib.GameProfile;
import gg.eventalerts.sdk.http.EAHTTP;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import gg.eventalerts.sdk.websocket.EAWebSocket;
import gg.eventalerts.sdk.websocket.SocketEventName;
import gg.eventalerts.sdk.websocket.message.event.SocketEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;


public class EventUtils implements ClientModInitializer {
    /**
     * Only use if it is absolutely impossible to access the mod instance through other (safer) means
     * <br>This is usually only necessary for mixins!
     */
    public static EventUtils MOD;
    @NotNull public static final Logger LOGGER = LogManager.getLogger(EventUtils.class, new PrefixMessageFactory());
    @Nullable public static final String MC_VERSION = FabricLoader.getInstance().getModContainer("minecraft")
            .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
            .orElse(null);
    @NotNull public static final String USER_AGENT = BuildProperties.MOD_NAME + "/" + BuildProperties.MOD_VERSION + " (MC/" + MC_VERSION + ")";
    @NotNull public static final String QUEUE_TEXT = "\n\n Per-server ranks get a higher priority in their respective queues. To receive such a rank, purchase one at\n store.invadedlands.net.\n\nTo leave a queue, use the command: /leavequeue.\n";
    @NotNull public static final MutableText MESSAGE_PREFIX = Text.literal(BuildProperties.MOD_NAME)
            .formatted(Formatting.BOLD)
            .fillStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF5AA42)))
            .append(Text.literal("§r »")
                    .fillStyle(Style.EMPTY.withBold(false).withColor(TextColor.fromRgb(0xB57C2F))));
    @NotNull public static final MutableText ERROR_MESSAGE_PREFIX = Text.literal(BuildProperties.MOD_NAME)
            .formatted(Formatting.BOLD)
            .formatted(Formatting.RED)
            .append(Text.literal("§r§4 »")
                    .fillStyle(Style.EMPTY.withBold(false)));

    @NotNull public final EventConfig config = new EventConfig();
    @NotNull public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    public EAHTTP http;
    public EAWebSocket webSocket;
    @NotNull public final EventAlertsApi api = new EventAlertsApi(this);
    @NotNull public final UpdateChecker updateChecker = new UpdateChecker(this);
    public KeybindManager keybindManager;
    @NotNull public final EventServerManager eventServerManager = new EventServerManager(this);
    /**
     * {@link EAEvent} or {@link EAFamousEvent}
     */
    @Nullable public Object lastEvent; //TODO turn into custom type with universal getters
    @NotNull public final Map<EventType, String> lastIps = new EnumMap<>(EventType.class);
    @NotNull public HidePlayersMode hidePlayersMode = HidePlayersMode.REVEALED;
    public int selectedGroup = 0;

    public EventUtils() {
        MOD = this;
    }

    @Nullable
    public static SemanticVersion getSemantic(@Nullable String string) {
        if (string != null) try {
            return SemanticVersion.parse(string);
        } catch (final VersionParsingException ignored) {}
        return null;
    }

    @Override
    public void onInitializeClient() {
        // Websocket
        setupSdk(null);

        // Command registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> CommandRegister.register(dispatcher));

        // Game closed
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            webSocket.close(1000, "Game closed");
            eventServerManager.removeAllEventServers();
        });

        // Update checker
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> updateChecker.notifyUpdate());

        // Fetch Event Alerts plus tags for local player
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                final UUID uuid = client.player.getUuid();
                LOGGER.info("[EventUtils] JOIN: scheduling Event Alerts fetch for local player uuid={}", uuid);
                api.scheduleFetchIfNeeded(uuid);
            } else {
                LOGGER.info("[EventUtils] JOIN: client.player is null, skipping fetch (will retry when tab list is opened)");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("[EventUtils] DISCONNECT: clearing Event Alerts cache");
            api.clearCache();
        });

        // Initialize keybind manager
        keybindManager = new KeybindManager(this);

        // Simple queue message
        ClientReceiveMessageEvents.ALLOW_GAME.register(((text, overlay) -> true));
        ClientReceiveMessageEvents.MODIFY_GAME.register(((text, overlay) -> {
            if (config.simpleQueueMessage && text.getString().contains(QUEUE_TEXT)) {
                final String original = text.getString();
                final MutableText resultText = Text.literal("");
                final Matcher matcher = java.util.regex.Pattern
                        .compile("([\\w -]+?Queue Position)\\s*:\\s*(\\d+)/(\\d+)")
                        .matcher(original);
                while (matcher.find()) {
                    if (!resultText.getSiblings().isEmpty()) resultText.append("\n");
                    resultText.append(Text.literal(matcher.group(1)).formatted(Formatting.GOLD).append(": ")
                            .append(Text.literal(matcher.group(2)).formatted(Formatting.YELLOW))
                            .append(Text.literal("/").formatted(Formatting.GOLD))
                            .append(Text.literal(matcher.group(3)).formatted(Formatting.YELLOW)));
                }
                if (!resultText.getSiblings().isEmpty()) return resultText;
            }
            // May need to manipulate later
            return text;
        }));
    }

    public void setupSdk(@Nullable String reason) {
        // HTTP
        http = new EAHTTP.Builder(USER_AGENT)
                .url(config.developerMode ? "http://localhost:8080/api/v1/" : "https://eventalerts.gg/api/v1/")
                .build();

        // Disconnect old socket
        if (webSocket != null) webSocket.close(1000, reason);

        // Connect new socket
        webSocket = new EAWebSocket.Builder(USER_AGENT)
                .url((config.developerMode ? "ws://localhost:9090" : "wss://eventalerts.gg") + "/api/v1/socket")
                .handler(
                        new EventCancelledListener(this),
                        new EventPostedListener(this),
                        new FamousEventPostedListener(this))
                .buildThenConnect();
    }

    public static boolean isNPC(@NotNull GameProfile profile) {
        //? if >=1.21.11 {
        /*final UUID uuid = profile.id();
        final String name = profile.name();
        *///?} else {
        final UUID uuid = profile.getId();
        final String name = profile.getName();
        //?}

        if (name.length() > 16 || name.length() < 3) return true;
        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) return true;

        final ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return false;
        return networkHandler.getPlayerListEntry(uuid) == null;
    }

    public static boolean isNPC(@NotNull String name, boolean bypass) {
        if (name.isEmpty()) return !MOD.config.hideNPCs || bypass;
        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) return !MOD.config.hideNPCs || bypass;
        final ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler != null) {
            final boolean inTabList = networkHandler.getPlayerList().stream()
                    .anyMatch(entry -> {
                        //? if >=1.21.11 {
                        /*final String entryName = entry.getProfile().name();
                        *///?} else {
                        final String entryName = entry.getProfile().getName();
                        //?}
                        return entryName.equalsIgnoreCase(name);
                    });
            if (!inTabList) return !MOD.config.hideNPCs || bypass;
        }
        return false;
    }

    public static boolean isNPC(@NotNull String name) {
        return isNPC(name, false);
    }

    public static boolean looksLikeNPC(@NotNull String name) {
        return name.contains("[") || name.contains("]") || name.contains(" ") || name.contains("-") || name.equals("§z");
    }

    public boolean isHidePlayersRevealed() {
        return hidePlayersMode == HidePlayersMode.REVEALED;
    }

    @Nullable
    public PlayerGroup getCurrentViewGroup() {
        return hidePlayersMode == HidePlayersMode.GROUP ? config.groups.get(selectedGroup) : null;
    }

    /**
     * True if the player (by lowercased name) should be visible with current view mode.
     * Caller must exclude main player.
     */
    public boolean isPlayerVisible(@NotNull String nameLower) {
        if (isHidePlayersRevealed() || config.whitelistedPlayers.contains(nameLower)) return true;
        final PlayerGroup group = getCurrentViewGroup();

        // NPC behavior: if the global hide toggle is OFF, NPCs should always stay visible.
        if (looksLikeNPC(nameLower)) {
            if (!config.hideNPCs) return true;
            if (group == null) return false;
            return group.isHideListedNpcs() != group.containsPlayer(nameLower);
        }

        if (group == null) return false; // no groups, hide mode: only whitelisted players are visible
        return group.isHideListedPlayers() != group.containsPlayer(nameLower);
    }

    /** True if the nametag for this visible player should be drawn (per-group setting when in group view). */
    public boolean shouldShowNametagFor(@NotNull String nameLower) {
        if (isHidePlayersRevealed()) return true;
        final PlayerGroup group = getCurrentViewGroup();
        if (group == null) return true; // hide-all with no groups: use default
        if (!group.containsPlayer(nameLower)) return true; // whitelist/NPC visibility: show nametag
        return group.isShowNametags();
    }

    @Contract(pure = true)
    public static int max(int @NotNull ... values) {
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
        // Create a test event
        final EAEvent testEvent = new EAEvent();
        testEvent.id = new ObjectId();
        testEvent.title = "Test Event";
        testEvent.description = "This is a simulated test event for testing the server list feature. Server: mc.hypixel.net";
        testEvent.time = new Date(System.currentTimeMillis() + (30 * 1000)); // +30 seconds
        testEvent.ip = "invadedlands.net";
        testEvent.prize = "$1000";
        testEvent.rolesNamed = Set.of(EAEvent.PingRole.MONEY);
        LOGGER.info("Simulating test event: {}", testEvent.toString());

        // Process event through handler
        new EventPostedListener(this).onMessage(new SocketEvent<>(SocketEventName.EVENT_POSTED, 1L, testEvent));

        // Set as last event for event info screen
        lastEvent = testEvent;
    }
}
