package cc.aabss.eventutils.config;

import cc.aabss.eventutils.stats.Stat;
import eu.okaeri.configs.OkaeriConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.stream.Collectors;


public class EUConfig extends OkaeriConfig {
    public static final int MIN_EVENT_SERVER_DISPLAY_MINUTES = 1;
    public static final int MAX_EVENT_SERVER_DISPLAY_MINUTES = 15;


    @Stat
    public boolean discord_rpc = Defaults.DISCORD_RPC;

    @Stat
    public boolean simple_queue_message = Defaults.SIMPLE_QUEUE_MESSAGE;

    @Stat
    public boolean update_checker = Defaults.UPDATE_CHECKER;

    @Stat
    public boolean confirm_window_close = Defaults.CONFIRM_WINDOW_CLOSE;

    @Stat
    public boolean confirm_disconnect = Defaults.CONFIRM_DISCONNECT;

    @Stat
    @NotNull public String default_famous_ip = Defaults.DEFAULT_FAMOUS_IP;

    @Stat
    public boolean bee_icons = Defaults.BEE_ICONS;

    @Range(from = MIN_EVENT_SERVER_DISPLAY_MINUTES, to = MAX_EVENT_SERVER_DISPLAY_MINUTES) @Stat
    public int event_server_display_minutes = Defaults.EVENT_SERVER_DISPLAY_MINUTES;

    @Stat
    @NotNull public Map<UUID, Group> groups = Defaults.groups();

    @Stat
    @NotNull public Map<EventType, EventSettings> event_settings = Defaults.eventSettings();

    @Stat
    public boolean developer_mode = Defaults.DEVELOPER_MODE;

    @Stat
    @NotNull public StandardLevel log_level = Defaults.LOG_LEVEL;


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

    /**
     * If a group with the same UUID already exists, it will be replaced
     */
    public void upsertGroup(@NotNull Group group) {
        groups.put(group.getUuid(), group);
        save();
    }

    @NotNull
    public EventSettings getEventSettings(@NotNull EventType type) {
        return event_settings.getOrDefault(type, new EventSettings());
    }

    @NotNull
    public EventSettings getEventSettingsOrCreate(@NotNull EventType type) {
        return event_settings.computeIfAbsent(type, t -> new EventSettings());
    }

    public void setEventServerDisplayMinutes(int eventServerDisplayMinutes) {
        final int before = this.event_server_display_minutes;
        // Don't use Math.clamp to support older Java versions
        this.event_server_display_minutes = Math.max(MIN_EVENT_SERVER_DISPLAY_MINUTES, Math.min(MAX_EVENT_SERVER_DISPLAY_MINUTES, eventServerDisplayMinutes));

        // Only save if changed
        if (before != this.event_server_display_minutes) save();
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
