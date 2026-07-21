package cc.aabss.eventutils;

import cc.aabss.eventutils.commands.CommandRegister;
import cc.aabss.eventutils.config.EUConfig;
import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.discordrpc.DiscordRPC;
import cc.aabss.eventutils.plustag.EventAlertsApi;
import cc.aabss.eventutils.sdk.EnrichedEvent;
import cc.aabss.eventutils.sdk.EventWrapper;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import cc.aabss.eventutils.websocket.listener.EventCancelledListener;
import cc.aabss.eventutils.websocket.listener.EventPostedListener;
import cc.aabss.eventutils.websocket.listener.FamousEventPostedListener;
import gg.eventalerts.sdk.http.EAHTTP;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.websocket.EAWebSocket;
import gg.eventalerts.sdk.websocket.SocketEventName;
import gg.eventalerts.sdk.websocket.message.event.SocketEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;
import xyz.srnyx.javautilities.objects.SemanticVersion;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;


public class EventUtils implements ClientModInitializer {
    @NotNull private static final Duration IN_EVENT_TIME = Duration.ofHours(12);
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

    @NotNull public final EUConfig config = new EUConfig();
    public EAHTTP http;
    public EAWebSocket webSocket;
    @NotNull public final EventAlertsApi api = new EventAlertsApi(this);
    @NotNull public final UpdateChecker updateChecker = new UpdateChecker(this);
    public DiscordRPC discordRPC;
    public KeybindManager keybindManager;
    @NotNull public final EventServerManager eventServerManager = new EventServerManager(this);
    @NotNull public final GroupManager groupManager = new GroupManager(this);
    @NotNull public final Map<EventType, EventWrapper> lastEvents = new EnumMap<>(EventType.class);
    @Nullable public EnrichedEvent inEvent;

    public EventUtils() {
        MOD = this;
        updateLogLevel(); // Need to wait for config to be set
    }

    @Nullable
    public static SemanticVersion getSemantic(@Nullable String string) {
        if (string == null) return null;
        return MiscUtility.handleException(() -> new SemanticVersion(string), NumberFormatException.class).orElse(null);
    }

    @Override
    public void onInitializeClient() {
        // Websocket
        setupSdk(null);

        // Discord RPC
        discordRPC = new DiscordRPC(this);

        // Command registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> CommandRegister.register(dispatcher));

        // Game closed
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            webSocket.shutdown("Game closed");
            discordRPC.close();
            eventServerManager.removeAllEventServers();
        });

        // On join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Update checker
            updateChecker.notifyUpdate();

            // Delay some stuff to wait for server info to be fully loaded
            MiscUtility.IO_SCHEDULER.schedule(() -> {
                // inEvent
                final ServerInfo server = client.getCurrentServerEntry();
                EventUtils.LOGGER.debug("[JOIN] server={}", server);
                if (server != null) {
                    final String ip = server.address.toLowerCase();
                    LOGGER.debug("[JOIN] retrieving event ip={}", ip);
                    http.events.retrieveMany(1, Map.of(
                                    "match", "any",
                                    "sort", "-created",
                                    "ip", ip,
                                    "description", ip,
                                    "title", ip,
                                    "prize", ip))
                            .onErrorReturnEmptyList()
                            .queue(events -> {
                                // Check event time
                                final EAEvent event = events != null && !events.isEmpty() ? events.get(0) : null;
                                if (event != null && event.time != null && System.currentTimeMillis() - event.time.getTime() > IN_EVENT_TIME.toMillis()) {
                                    LOGGER.debug("[JOIN] event too old, ignoring: {}", event);
                                    inEvent = null;
                                } else {
                                    inEvent = event != null ? new EnrichedEvent(this, event) : null;
                                }
                                LOGGER.debug("[JOIN] inEvent={}", inEvent);

                                // Then update presence
                                discordRPC.refresh();
                            });
                    return;
                }

                // Not in multiplayer server: Update Discord presence immediately
                discordRPC.refresh();
            }, 1, TimeUnit.SECONDS);
        });

        // On leave
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear API cache
            LOGGER.debug("[EventUtils] DISCONNECT: clearing Event Alerts cache");
            api.clearCache();

            // inEvent
            inEvent = null;

            // DiscordRPC (delay for status to update correctly)
            MiscUtility.IO_SCHEDULER.schedule(() -> discordRPC.refresh(), 1, TimeUnit.SECONDS);
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

    public void updateLogLevel() {
        // Get level
        Level level = Level.toLevel(config.logLevel.name());
        if (level == Level.INFO && config.developerMode) level = Level.DEBUG;

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();
        final String name = LOGGER.getName();

        // Create LoggerConfig if doesn't exist
        LoggerConfig loggerConfig = config.getLoggerConfig(name);
        if (!loggerConfig.getName().equals(name)) {
            loggerConfig = new LoggerConfig(name, level, true);
            config.addLogger(name, loggerConfig);
        } else {
            loggerConfig.setLevel(level);
        }

        context.updateLoggers();
    }

    public void setupSdk(@Nullable String reason) {
        // HTTP
        http = new EAHTTP.Builder(USER_AGENT)
                .url(config.developerMode ? "http://localhost:8080/api/v1/" : "https://eventalerts.gg/api/v1/")
                .build();

        // Shutdown old socket
        if (webSocket != null) webSocket.shutdown(reason);

        // Connect new socket
        webSocket = new EAWebSocket.Builder(USER_AGENT)
                .url((config.developerMode ? "ws://localhost:9090" : "wss://eventalerts.gg") + "/api/v1/socket")
                .handler(
                        new EventCancelledListener(this),
                        new EventPostedListener(this),
                        new FamousEventPostedListener(this))
                .buildThenConnect();
    }

    public static boolean isNpc(@NotNull UUID uuid) {
        final ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return networkHandler != null && networkHandler.getPlayerListEntry(uuid) == null;
    }

    public static boolean isNpc(@Nullable String name) {
        // Invalid name = NPC
        if (name == null || name.isEmpty() || !name.matches("^[a-zA-Z0-9_]{3,16}$")) return true;

        // Check if name in player-list
        final ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return networkHandler != null && networkHandler.getPlayerList().stream()
                .noneMatch(entry -> new VersionedGameProfile(entry.getProfile()).getName().equalsIgnoreCase(name));
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
        LOGGER.debug("Simulating test event: {}", testEvent.toString());

        // Process event through handler
        new EventPostedListener(this).onMessage(new SocketEvent<>(SocketEventName.EVENT_POSTED, 1L, testEvent));

        // Set as last event for event info screen
        lastEvents.put(EventType.MONEY, new EventWrapper(this, testEvent));
    }
}
