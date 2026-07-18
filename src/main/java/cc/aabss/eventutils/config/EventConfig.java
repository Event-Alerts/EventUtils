package cc.aabss.eventutils.config;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;
import org.apache.logging.log4j.Level;
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
    public boolean eventServersEnabled;
    public int eventServerDisplayMinutes;
    @NotNull public String defaultFamousIp;
    @NotNull public Map<UUID, Group> groups;
    public boolean developerMode;
    @NotNull public Level logLevel;
    @NotNull public final List<EventType> eventTypes;
    @NotNull public final List<EventType> eventServerTypes;
    @NotNull public final Map<EventType, NotificationSound> notificationSounds;

    public EventConfig(@NotNull EventUtils mod) {
        super(new File(FabricLoader.getInstance().getConfigDir().toFile(), "eventutils.json"));

        // Create empty file if it doesn't exist
        boolean created = false;
        if (!file.exists()) {
            json = new JsonObject();
            if (EventUtils.getSemantic(BuildProperties.MOD_VERSION) != null) json.addProperty("version", BuildProperties.MOD_VERSION);
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
        eventServersEnabled = get("event_servers_enabled", Defaults.EVENT_SERVERS_ENABLED);
        eventServerDisplayMinutes = get("event_server_display_minutes", Defaults.EVENT_SERVER_DISPLAY_MINUTES);
        groups = new LinkedHashMap<>(get("groups", Defaults.groups(), new TypeToken<Map<UUID, Group>>(){}.getType()));
        developerMode = get("developer_mode", Defaults.DEVELOPER_MODE);
        logLevel = get("log_level", Defaults.LOG_LEVEL);
        eventTypes = get("notifications", Defaults.eventTypes(), new TypeToken<List<EventType>>(){}.getType());
        eventServerTypes = get("event_server_types", Defaults.eventServerTypes(), new TypeToken<List<EventType>>(){}.getType());
        notificationSounds = get("notification_sounds", Defaults.notificationSounds(), new TypeToken<Map<EventType, NotificationSound>>(){}.getType());

        // Save if created (default values)
        if (created) save();

        // Log level
        mod.setLogLevel(logLevel);
    }

    private void update() {
        // Get old version
        final String oldVersionString = get("version", "1.4.0");
        final SemanticVersion oldVersion = EventUtils.getSemantic(oldVersionString);
        if (oldVersion == null) return;

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
            set("whitelisted_players", get("whitelisted-players", List.<String>of(), new TypeToken<List<String>>(){}.getType()).stream()
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

        // 2.3.0 or older
        if (oldVersion.compareTo((Version) new SemanticVersionImpl(new int[]{2, 3, 0}, null, null)) <= 0) {
            // use_testing_api -> developer_mode
            update("use_testing_api", "developer_mode", Boolean.class);

            // Flat hide values -> default group
            final Group group = new Group()
                    .setName("Legacy Group")
                    .setPlayers(get("whitelisted_players", List.of(), new TypeToken<List<String>>(){}.getType()))
                    .setEntities(get("hidden_entity_types", List.of(), new TypeToken<List<String>>(){}.getType()))
                    .setPlayerMode(Group.Mode.SHOW)
                    .setEntityMode(Group.Mode.HIDE)
                    .setRadius(get("hide_players_radius", Integer.class));
            set("groups", Map.of(UUID.randomUUID(), group));
            remove("whitelisted_players");
            remove("hidden_entity_types");
            remove("hide_players_radius");
            remove("hide_npcs");
        }

        // --- ADD NEW MIGRATIONS ABOVE THIS LINE ---
        // Make sure to update the "fallback version" below when adding new migrations!
        // The fallback version should always be one patch version higher than the latest migration.
        // We need this fallback version to prevent semantic parsing issues when using "dev" version.

        // Update version
        set("version", EventUtils.getSemantic(BuildProperties.MOD_VERSION) != null ? BuildProperties.MOD_VERSION : "2.3.1");
        save();
    }

    private void update(@NotNull String oldKey, @NotNull String newKey, @NotNull Type type) {
        set(newKey, get(oldKey, type));
        remove(oldKey);
    }

    @NotNull
    public NotificationSound getNotificationSound(@NotNull EventType type) {
        return notificationSounds.getOrDefault(type, NotificationSound.ALERT);
    }

    public int getEventServerDisplayMinutes() {
        if (eventServerDisplayMinutes < 1) eventServerDisplayMinutes = 1;
        if (eventServerDisplayMinutes > 15) eventServerDisplayMinutes = 15;
        return eventServerDisplayMinutes;
    }

    // Collections need to have methods to create new instances of the collection!
    public static class Defaults {
        public static final boolean DISCORD_RPC = true;
        public static final boolean AUTO_TP = false;
        public static final boolean SIMPLE_QUEUE_MESSAGE = false;
        public static final boolean UPDATE_CHECKER = true;
        public static final boolean CONFIRM_WINDOW_CLOSE = true;
        public static final boolean CONFIRM_DISCONNECT = true;
        public static final boolean EVENT_SERVERS_ENABLED = true;
        public static final int EVENT_SERVER_DISPLAY_MINUTES = 5;
        @NotNull public static final String DEFAULT_FAMOUS_IP = "play.invadedlands.net";
        @NotNull public static final Map<UUID, Group> DEFAULT_GROUPS = Map.of(UUID.randomUUID(), new Group().setName("Hide All Players"));
        public static final boolean DEVELOPER_MODE = false;
        @NotNull public static final Level LOG_LEVEL = Level.INFO;
        @NotNull private static final List<EventType> EVENT_TYPES = List.of(EventType.values());
        @NotNull private static final List<EventType> EVENT_SERVER_TYPES = List.of(EventType.values());
        @NotNull private static final Map<EventType, NotificationSound> NOTIFICATION_SOUNDS = Arrays.stream(EventType.values())
                .collect(HashMap::new, (map, type) -> map.put(type, NotificationSound.ALERT), HashMap::putAll);

        @NotNull
        public static Map<UUID, Group> groups() {
            return new HashMap<>(DEFAULT_GROUPS);
        }
        @NotNull
        public static List<EventType> eventTypes() {
            return new ArrayList<>(EVENT_TYPES);
        }
        @NotNull
        public static List<EventType> eventServerTypes() {
            return new ArrayList<>(EVENT_SERVER_TYPES);
        }
        @NotNull
        public static Map<EventType, NotificationSound> notificationSounds() {
            return new HashMap<>(NOTIFICATION_SOUNDS);
        }
    }
}
