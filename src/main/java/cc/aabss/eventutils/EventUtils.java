package cc.aabss.eventutils;

import cc.aabss.eventutils.commands.CommandRegister;
import cc.aabss.eventutils.config.EUConfig;
import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.config.migrations.*;
import cc.aabss.eventutils.config.serdes.EntityTypeSerializer;
import cc.aabss.eventutils.config.serdes.EventSettingsSerializer;
import cc.aabss.eventutils.config.serdes.GroupSerializer;
import cc.aabss.eventutils.discordrpc.DiscordRPC;
import cc.aabss.eventutils.manager.AuthManager;
import cc.aabss.eventutils.manager.DiscordLinkManager;
import cc.aabss.eventutils.cache.CacheManager;
import cc.aabss.eventutils.manager.EventServerManager;
import cc.aabss.eventutils.manager.GroupManager;
import cc.aabss.eventutils.manager.KeybindManager;
import cc.aabss.eventutils.sdk.EnrichedEvent;
import cc.aabss.eventutils.sdk.EventWrapper;
import cc.aabss.eventutils.stats.Stats;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import cc.aabss.eventutils.websocket.listener.EventCancelledListener;
import cc.aabss.eventutils.websocket.listener.EventPostedListener;
import cc.aabss.eventutils.websocket.listener.FamousEventPostedListener;
import dev.kikugie.fletching_table.fabric.Entrypoint;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


@Entrypoint("client")
public class EventUtils implements ClientModInitializer {
    @NotNull private static final Duration IN_EVENT_TIME = Duration.ofHours(12);
    @NotNull public static final String QUEUE_TEXT = "\n\n Per-server ranks get a higher priority in their respective queues. To receive such a rank, purchase one at\n store.invadedlands.net.\n\nTo leave a queue, use the command: /leavequeue.\n";
    @NotNull public static final MutableComponent MESSAGE_PREFIX = Component.literal(BuildProperties.MOD_NAME)
            .withStyle(ChatFormatting.BOLD)
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF5AA42)))
            .append(Component.literal("§r »")
                    .withStyle(Style.EMPTY.withBold(false).withColor(TextColor.fromRgb(0xB57C2F))));
    @NotNull public static final MutableComponent ERROR_MESSAGE_PREFIX = Component.literal(BuildProperties.MOD_NAME)
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.RED)
            .append(Component.literal("§r§4 »")
                    .withStyle(Style.EMPTY.withBold(false)));

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
    @NotNull public final CacheManager cacheManager = new CacheManager(this);
    @NotNull public final AuthManager authManager = new AuthManager(this);
    @NotNull public final DiscordLinkManager discordLinkManager = new DiscordLinkManager(this);
    @NotNull public final UpdateChecker updateChecker = new UpdateChecker(this);
    @NotNull public final Stats stats = new Stats(this);
    public DiscordRPC discordRPC;
    public KeybindManager keybindManager;
    @NotNull public final GroupManager groupManager = new GroupManager(this);
    @NotNull public final EventServerManager eventServerManager = new EventServerManager(this);
    @NotNull public final Map<EventType, EventWrapper> lastEvents = new EnumMap<>(EventType.class);
    @Nullable public EnrichedEvent inEvent;

    public EventUtils() {
        MOD = this;

        // Load config
        config.configure(opt -> {
                    opt.configurer(
                            new JsonGsonConfigurer(),

                            // Okaeri serdes
                            new SerdesCommons(),

                            // Custom serdes
                            new EntityTypeSerializer(),
                            new EventSettingsSerializer(),
                            new GroupSerializer());

                    opt.bindFile(new File(FabricLoader.getInstance().getConfigDir().toFile(), "eventutils.json"));
                    opt.removeOrphans(true);
                });
        config.saveDefaults(); // Basically just creates file if it doesn't exist
        config.load(); // Initial load (for migrations)
        config.migrateInternalState( // Migrations
                new C0001_Rename_kebab_case_to_snake_case(),
                new C0002_Rename_notifications_event_types(),
                new C0003_Flat_alerts_to_EventSettings(),
                new C0004_Flat_hiding_to_Groups(),
                new C0005_use_testing_api_to_developer_mode());
        config.save(); // Manually save in-case no migrations occured

        // Clamp event_server_display_minutes
        config.setEventServerDisplayMinutes(config.event_server_display_minutes);
        // Add UUIDs to Groups
        for (final Map.Entry<UUID, Group> entry : config.groups.entrySet()) entry.getValue().setUuid(entry.getKey());
        // Validate groups (unique names)
        boolean updated = false;
        final Set<String> groupNames = new HashSet<>();
        for (final Group group : new HashSet<>(config.groups.values())) {
            if (groupNames.add(group.getName().toLowerCase())) continue;
            EventUtils.LOGGER.error("Removing duplicate group: {}", group);
            config.groups.remove(group.getUuid());
            updated = true;
        }
        if (updated) config.save(); // Save if a group was removed

        // Need to wait for config to be loaded
        updateLogLevel();
    }

    @Override
    public void onInitializeClient() {
        // SDK
        setupSdk(null);

        // Authenticate
        authManager.authenticate().queue(
                response -> {
                    // Discord RPC
                    discordRPC = new DiscordRPC(this);
                },
                t -> {
                    // Discord RPC
                    discordRPC = new DiscordRPC(this);
                    LOGGER.error("Failed to authenticate with Event Alerts: {}", t.getMessage());
                });

        // Command registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> CommandRegister.register(this, dispatcher));

        // Game closed
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            authManager.shutdown();
            discordLinkManager.cancel();
            discordRPC.close();
            eventServerManager.removeAllEventServers();
            webSocket.shutdown("Game closed");
        });

        // On join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Update checker
            updateChecker.notifyUpdate();

            // Delay some stuff to wait for server info to be fully loaded
            MiscUtility.IO_SCHEDULER.schedule(() -> {
                // Populate players cache
                final ClientPacketListener packetListener = client.getConnection();
                if (packetListener != null) cacheManager
                        .players()
                        .get(packetListener.getListedOnlinePlayers().stream()
                                .map(entry -> new VersionedGameProfile(entry.getProfile()).getId())
                                .collect(Collectors.toSet()))
                        .queue();

                // inEvent
                final ServerData server = client.getCurrentServer();
                EventUtils.LOGGER.debug("[JOIN] server={}", server != null ? server.ip : "null");
                if (server != null) {
                    final String ip = server.ip.toLowerCase();
                    LOGGER.debug("[JOIN] retrieving event ip={}", ip);
                    http.events.retrieveMany(1, null, Map.of(
                                    "match", "any",
                                    "sort", "-created",
                                    "ip", ip,
                                    "description", ip,
                                    "title", ip,
                                    "prize", ip))
                            .onErrorReturnEmptyList()
                            .queue(events -> {
                                // Check event time (don't use getFirst to support older Java versions)
                                final EAEvent event = events != null && !events.isEmpty() ? events.get(0) : null;
                                if (event != null && event.time != null && System.currentTimeMillis() - event.time.getTime() > IN_EVENT_TIME.toMillis()) {
                                    LOGGER.debug("[JOIN] event too old, ignoring: {}", event);
                                    inEvent = null;
                                } else {
                                    inEvent = event != null ? new EnrichedEvent(this, event) : null;
                                }
                                LOGGER.debug("[JOIN] inEvent={}", inEvent);

                                // Then update Discord presence
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
            // Clear cache
            LOGGER.debug("[CACHE] DISCONNECT: clearing cache");
            cacheManager.clearAll();

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
            if (config.simple_queue_message && text.getString().contains(QUEUE_TEXT)) {
                final String original = text.getString();
                final MutableComponent resultComponent = Component.literal("");
                final Matcher matcher = java.util.regex.Pattern
                        .compile("([\\w -]+?Queue Position)\\s*:\\s*(\\d+)/(\\d+)")
                        .matcher(original);
                while (matcher.find()) {
                    if (!resultComponent.getSiblings().isEmpty()) resultComponent.append("\n");
                    resultComponent.append(Component.literal(matcher.group(1)).withStyle(ChatFormatting.GOLD).append(": ")
                            .append(Component.literal(matcher.group(2)).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("/").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(matcher.group(3)).withStyle(ChatFormatting.YELLOW)));
                }
                if (!resultComponent.getSiblings().isEmpty()) return resultComponent;
            }
            // May need to manipulate later
            return text;
        }));
    }

    public void updateLogLevel() {
        // Get level
        Level level = Level.toLevel(config.log_level.name());
        if (level == Level.INFO && config.developer_mode) level = Level.DEBUG;

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
                .url(config.developer_mode ? "http://localhost:8080/api/v1/" : "https://eventalerts.gg/api/v1/")
                .build();

        // Shutdown old socket
        if (webSocket != null) webSocket.shutdown(reason);

        // Connect new socket
        webSocket = new EAWebSocket.Builder(USER_AGENT)
                .url((config.developer_mode ? "ws://localhost:9090" : "wss://eventalerts.gg") + "/api/v1/socket")
                .handler(
                        new EventCancelledListener(this),
                        new EventPostedListener(this),
                        new FamousEventPostedListener(this))
                .buildThenConnect();
    }

    public static boolean isNpc(@NotNull UUID uuid) {
        final ClientPacketListener packetListener = Minecraft.getInstance().getConnection();
        return packetListener != null && packetListener.getPlayerInfo(uuid) == null;
    }

    public static boolean isNpc(@Nullable String name) {
        // Invalid name = NPC
        if (name == null || name.isEmpty() || !name.matches("^[a-zA-Z0-9_]{3,16}$")) return true;

        // Check if name in player-list
        final ClientPacketListener packetListener = Minecraft.getInstance().getConnection();
        return packetListener != null && packetListener.getListedOnlinePlayers().stream()
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
        return Language.getInstance().getOrDefault(key);
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
