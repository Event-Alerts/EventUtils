package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.Versions;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;

import net.minecraft.entity.EntityType;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;


public class EventConfig extends FileLoader {
    public boolean discordRpc;
    public boolean autoTp;
    public boolean simpleQueueMessage;
    public boolean updateChecker;
    public boolean confirmWindowClose;
    public boolean confirmDisconnect;
    public int hidePlayersRadius;
    @NotNull public String defaultFamousIp;
    @NotNull public List<EntityType<?>> hiddenEntityTypes;
    @NotNull public List<String> whitelistedPlayers;
    public boolean useTestingApi;
    @NotNull public final List<EventType> eventTypes;
    @NotNull public final Map<EventType, NotificationSound> notificationSounds;

    public EventConfig() {
        super(new File(FabricLoader.getInstance().getConfigDir().toFile(), "eventutils.json"));

        // Create empty file if it doesn't exist
        boolean created = false;
        if (!file.exists()) {
            json = new JsonObject();
            json.addProperty("version", Versions.EU_VERSION);
            created = true;
        } else {
            load();
            update();
        }

        // Get values
        discordRpc = get("discord_rpc", Defaults.DISCORD_RPC);
        autoTp = get("auto_tp", Defaults.AUTO_TP);
        simpleQueueMessage = get("simple_queue_message", Defaults.SIMPLE_QUEUE_MESSAGE);
        updateChecker = get("update_checker", Defaults.UPDATE_CHECKER);
        confirmWindowClose = get("confirm_window_close", Defaults.CONFIRM_WINDOW_CLOSE);
        confirmDisconnect = get("confirm_disconnect", Defaults.CONFIRM_DISCONNECT);
        defaultFamousIp = get("default_famous_ip", Defaults.DEFAULT_FAMOUS_IP);
        hidePlayersRadius = get("hide_players_radius", Defaults.HIDE_PLAYERS_RADIUS);
        hiddenEntityTypes = get("hidden_entity_types", Defaults.hiddenEntityTypes(), new TypeToken<List<EntityType<?>>>(){}.getType());
        whitelistedPlayers = get("whitelisted_players", Defaults.whitelistedPlayers(), new TypeToken<List<String>>(){}.getType());
        useTestingApi = get("use_testing_api", Defaults.USE_TESTING_API);
        eventTypes = get("notifications", Defaults.eventTypes(), new TypeToken<List<EventType>>(){}.getType());
        notificationSounds = get("notification_sounds", Defaults.notificationSounds(), new TypeToken<Map<EventType, NotificationSound>>(){}.getType());

        // Save if created (default values)
        if (created) save();
    }

    private void update() {
        // Get old version
        final String oldVersionString = get("version", "1.4.0");
        final SemanticVersion oldVersion = Versions.getSemantic(oldVersionString);
        if (oldVersion == null) {
            EventUtils.LOGGER.error("Failed to parse config version: {}", oldVersionString);
            return;
        }

        // Older than 2.0.0
        if (oldVersion.compareTo((Version) new SemanticVersionImpl(new int[]{2, 0, 0}, null, null)) < 0) {
            update("discord-rpc", "discord_rpc", Boolean.class);
            update("auto-tp", "auto_tp", Boolean.class);
            update("simple-queue-msg", "simple_queue_message", Boolean.class);
            update("update-checker", "update_checker", Boolean.class);
            update("confirm-window-close", "confirm_window_close", Boolean.class);
            update("confirm-disconnect", "confirm_disconnect", Boolean.class);
            update("default-famous-ip", "default_famous_ip", String.class);

            // whitelisted_players
            set("whitelisted_players", get("whitelisted-players", Defaults.WHITELISTED_PLAYERS, new TypeToken<List<String>>(){}.getType()).stream()
                    .map(String::toLowerCase)
                    .toList());
            remove("whitelisted-players");

            // notifications
            final Set<EventType> types = new HashSet<>();
            for (final EventType type : EventType.values()) {
                final String key = type.name().toLowerCase().replace("_", "-") + "-event";
                if (get(key, true)) types.add(type);
                remove(key);
            }
            set("notifications", types);
        }

        // 2.0.7 or older
        if (oldVersion.compareTo((Version) new SemanticVersionImpl(new int[]{2, 0, 7}, null, null)) <= 0) {
            final Integer radius = get("hide_players_radius", TypeToken.of(Integer.class).getType());
            if (radius != null && radius == 1) set("hide_players_radius", 0);
        }

        // Update version
        set("version", Versions.EU_VERSION);
        save();
    }

    private void update(@NotNull String oldKey, @NotNull String newKey, @NotNull Type type) {
        set(newKey, get(oldKey, type));
        remove(oldKey);
    }

    @NotNull
    public String getWebsocketHost() {
        return useTestingApi ? "ws://localhost:9090" : "wss://eventalerts.gg";
    }

    @NotNull
    public String getEventSkinsAPIHost() {
        return "https://eventskins.piscies.pvtylabs.com";
    }

    @NotNull
    public NotificationSound getNotificationSound(@NotNull EventType type) {
        return notificationSounds.getOrDefault(type, NotificationSound.ALERT);
    }

    // Collections need to have methods to create new instances of the collection!
    public static class Defaults {
        public static final boolean DISCORD_RPC = true;
        public static final boolean AUTO_TP = false;
        public static final boolean SIMPLE_QUEUE_MESSAGE = false;
        public static final boolean UPDATE_CHECKER = true;
        public static final boolean CONFIRM_WINDOW_CLOSE = true;
        public static final boolean CONFIRM_DISCONNECT = true;
        public static final int HIDE_PLAYERS_RADIUS = 0;
        @NotNull public static final String DEFAULT_FAMOUS_IP = "play.invadedlands.net";
        @NotNull private static final List<EntityType<?>> HIDDEN_ENTITY_TYPES = List.of(EntityType.GLOW_ITEM_FRAME);
        @NotNull private static final List<String> HIDDEN_ENTITY_TYPES_STRING = List.of("minecraft:glow_item_frame");
        @NotNull private static final List<String> WHITELISTED_PLAYERS = List.of("skeppy", "badboyhalo");
        public static final boolean USE_TESTING_API = false;
        @NotNull private static final List<EventType> EVENT_TYPES = List.of(EventType.values());
        @NotNull private static final Map<EventType, NotificationSound> NOTIFICATION_SOUNDS = Arrays.stream(EventType.values())
                .collect(HashMap::new, (map, type) -> map.put(type, NotificationSound.ALERT), HashMap::putAll);

        @NotNull
        public static List<EntityType<?>> hiddenEntityTypes() {
            return new ArrayList<>(HIDDEN_ENTITY_TYPES);
        }
        @NotNull
        public static List<String> hiddenEntityTypesString() {
            return new ArrayList<>(HIDDEN_ENTITY_TYPES_STRING);
        }
        @NotNull
        public static List<String> whitelistedPlayers() {
            return new ArrayList<>(WHITELISTED_PLAYERS);
        }
        @NotNull
        public static List<EventType> eventTypes() {
            return new ArrayList<>(EVENT_TYPES);
        }
        @NotNull
        public static Map<EventType, NotificationSound> notificationSounds() {
            return new HashMap<>(NOTIFICATION_SOUNDS);
        }
    }
}
