package cc.aabss.eventutils.config;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.EventUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import xyz.srnyx.javautilities.objects.SemanticVersion;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;


public class EUConfig extends FileLoader {
    public static final int MIN_EVENT_SERVER_DISPLAY_MINUTES = 1;
    public static final int MAX_EVENT_SERVER_DISPLAY_MINUTES = 15;


    public boolean discordRpc;
    public boolean simpleQueueMessage;
    public boolean updateChecker;
    public boolean confirmWindowClose;
    public boolean confirmDisconnect;
    @NotNull public String defaultFamousIp;
    public boolean beeIcons;
    @Range(from = MIN_EVENT_SERVER_DISPLAY_MINUTES, to = MAX_EVENT_SERVER_DISPLAY_MINUTES) public int eventServerDisplayMinutes;
    @NotNull public final Map<UUID, Group> groups;

    @NotNull public final Map<EventType, EventSettings> eventSettings;

    public boolean developerMode;
    @NotNull public StandardLevel logLevel;


    public EUConfig() {
        super(new File(FabricLoader.getInstance().getConfigDir().toFile(), "eventutils.json"));

        // Create empty file if it doesn't exist
        boolean created = false;
        if (!file.exists()) {
            // File doesn't exist, create it
            json = new JsonObject();
            if (EventUtils.getSemantic(BuildProperties.MOD_VERSION) != null) json.addProperty("version", BuildProperties.MOD_VERSION);
            created = true;
        } else {
            // File already exists
            load();
            migrate();
        }

        // Get values
        discordRpc = get("discord_rpc", Defaults.DISCORD_RPC);
        simpleQueueMessage = get("simple_queue_message", Defaults.SIMPLE_QUEUE_MESSAGE);
        updateChecker = get("update_checker", Defaults.UPDATE_CHECKER);
        confirmWindowClose = get("confirm_window_close", Defaults.CONFIRM_WINDOW_CLOSE);
        confirmDisconnect = get("confirm_disconnect", Defaults.CONFIRM_DISCONNECT);
        defaultFamousIp = get("default_famous_ip", Defaults.DEFAULT_FAMOUS_IP);
        beeIcons = get("bee_icons", Defaults.BEE_ICONS);
        setEventServerDisplayMinutes(get("event_server_display_minutes", Defaults.EVENT_SERVER_DISPLAY_MINUTES));
        groups = get("groups", Defaults.groups(), new TypeToken<Map<UUID, Group>>(){}.getType());
        eventSettings = get("event_settings", Defaults.eventSettings(), new TypeToken<Map<EventType, EventSettings>>(){}.getType());
        developerMode = get("developer_mode", Defaults.DEVELOPER_MODE);
        logLevel = get("log_level", Defaults.LOG_LEVEL);

        // Validate groups (unique names)
        final Set<String> groupNames = new HashSet<>();
        for (final Group group : new HashSet<>(groups.values())) {
            if (groupNames.add(group.getName().toLowerCase())) continue;
            EventUtils.LOGGER.error("Removing duplicate group: {}", group.getName());
            groups.values().remove(group);
        }

        // Save if created (default values)
        if (created) save();
    }

    private void migrate() {
        // Get old version (ignore before 1.0.0 [dev])
        final String oldVersionString = get("version", "1.4.0");
        final SemanticVersion oldVersion = EventUtils.getSemantic(oldVersionString);
        if (oldVersion == null || oldVersion.compareTo(new SemanticVersion(1, 0, 0)) < 0) return;

        // Older than 2.0.0
        if (oldVersion.compareTo(new SemanticVersion(2, 0, 0)) < 0) {
            EventUtils.LOGGER.warn("EventUtils config ({}) is older than 2.0.0, migrating to new format", oldVersionString);

            update("discord-rpc", "discord_rpc", Boolean.class);
            update("auto-tp", "auto_tp", Boolean.class);
            update("simple-queue-msg", "simple_queue_message", Boolean.class);
            update("update-checker", "update_checker", Boolean.class);
            update("confirm-window-close", "confirm_window_close", Boolean.class);
            update("confirm-disconnect", "confirm_disconnect", Boolean.class);
            update("default-famous-ip", "default_famous_ip", String.class);

            // whitelisted_players
            final List<String> whitelistedPlayers = remove("whitelisted_players", new TypeToken<List<String>>(){}.getType());
            if (whitelistedPlayers != null) set("whitelisted-players", whitelistedPlayers.stream()
                    .map(String::toLowerCase)
                    .toList());

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
        if (oldVersion.compareTo(new SemanticVersion(2, 0, 7)) <= 0) {
             EventUtils.LOGGER.warn("EventUtils config ({}) is 2.0.7 or older, migrating to new format", oldVersionString);

            final Integer radius = get("hide_players_radius", TypeToken.of(Integer.class).getType());
            if (radius != null && radius == 1) set("hide_players_radius", 0);
        }

        // 2.3.0 or older
        if (oldVersion.compareTo(new SemanticVersion(2, 3, 0)) <= 0) {
            EventUtils.LOGGER.warn("EventUtils config ({}) is 2.3.0 or older, migrating to new format", oldVersionString);

            // Flat alerts -> EventSettings
            final Map<EventType, EventSettings> newEventSettings = new HashMap<>();
            final Boolean autoTp = remove("auto_tp", Boolean.class);
            final Set<EventType> notificationsEnabled = remove("notifications", new TypeToken<Set<EventType>>(){}.getType());
            final Map<EventType, NotificationSound> notificationSounds = remove("notification_sounds", new TypeToken<Map<EventType, NotificationSound>>(){}.getType());
            final Boolean eventServersEnabledGlobal = remove("event_servers_enabled", Boolean.class);
            final Set<EventType> eventServersEnabled = remove("event_server_types", new TypeToken<Set<EventType>>(){}.getType());
            for (final EventType type : EventType.values()) {
                final EventSettings settings = new EventSettings();
                if (autoTp != null) settings.autoTp = autoTp;
                if (notificationsEnabled != null) settings.toasts = notificationsEnabled.contains(type);
                if (notificationSounds != null) settings.sound = notificationSounds.getOrDefault(type, NotificationSound.ALERT);
                if (eventServersEnabledGlobal != null && !eventServersEnabledGlobal) {
                    // Global disabled -> all disabled
                    settings.serverList = false;
                } else if (eventServersEnabled != null) {
                    // Individual setting
                    settings.serverList = eventServersEnabled.contains(type);
                }
                newEventSettings.put(type, settings);
            }
            set("event_settings", newEventSettings);

            // Flat hide values -> Group
            final Group group = new Group()
                    .setName("Legacy Group")
                    .setPlayerMode(Group.Mode.SHOW)
                    .setEntityMode(Group.Mode.HIDE);
            final List<String> whitelistedPlayers = remove("whitelisted_players", new TypeToken<List<String>>(){}.getType());
            if (whitelistedPlayers != null) group.setPlayers(whitelistedPlayers);
            final List<String> hiddenEntityTypes = remove("hidden_entity_types", new TypeToken<List<String>>(){}.getType());
            if (hiddenEntityTypes != null) group.setEntities(hiddenEntityTypes);
            final Integer hidePlayersRadius = remove("hide_players_radius", Integer.class);
            if (hidePlayersRadius != null) group.setRadius(hidePlayersRadius);
            final Boolean hideNpcs = remove("hide_npcs", Boolean.class);
            if (hideNpcs != null) group.setNpcMode(hideNpcs ? Group.Mode.HIDE : Group.Mode.SHOW);
            set("groups", Map.of(UUID.randomUUID(), group));

            // use_testing_api -> developer_mode
            update("use_testing_api", "developer_mode", Boolean.class);
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
        set(newKey, remove(oldKey, type));
    }

    @NotNull
    public List<String> getGroupNames() {
        return groups.values().stream()
                .map(Group::getName)
                .toList();
    }

    @Nullable
    public Group getGroupByName(@NotNull String name) {
        return groups.values().stream()
                .filter(group -> group.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public EventSettings getEventSettings(@NotNull EventType type) {
        return eventSettings.getOrDefault(type, new EventSettings());
    }

    public void setEventServerDisplayMinutes(int eventServerDisplayMinutes) {
        // Don't use Math.clamp to support older Java versions
        this.eventServerDisplayMinutes = Math.max(MIN_EVENT_SERVER_DISPLAY_MINUTES, Math.min(MAX_EVENT_SERVER_DISPLAY_MINUTES, eventServerDisplayMinutes));
    }

    // Collections/Maps need to have methods to create new instances of the collection!
    public static class Defaults {
        public static final boolean DISCORD_RPC = true;
        public static final boolean SIMPLE_QUEUE_MESSAGE = false;
        public static final boolean UPDATE_CHECKER = true;
        public static final boolean CONFIRM_WINDOW_CLOSE = true;
        public static final boolean CONFIRM_DISCONNECT = true;
        @NotNull public static final String DEFAULT_FAMOUS_IP = "play.invadedlands.net";
        public static final boolean BEE_ICONS = true;
        public static final int EVENT_SERVER_DISPLAY_MINUTES = 5;
        @NotNull private static final Map<UUID, Group> GROUPS = Map.of(UUID.randomUUID(), new Group().setName("Hide All Players"));

        @NotNull private static final Map<EventType, EventSettings> EVENT_SETTINGS = Arrays.stream(EventType.values())
                .collect(Collectors.toMap(type -> type, type -> new EventSettings(), (a, b) -> a, () -> new EnumMap<>(EventType.class)));

        public static final boolean DEVELOPER_MODE = false;
        @NotNull public static final StandardLevel LOG_LEVEL = Level.INFO.getStandardLevel();

        @NotNull
        public static Map<UUID, Group> groups() {
            return new HashMap<>(GROUPS);
        }
        @NotNull
        public static Map<EventType, EventSettings> eventSettings() {
            return new HashMap<>(EVENT_SETTINGS);
        }
    }
}
