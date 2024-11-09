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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
    @NotNull public final List<EventType> eventTypes;

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
        hiddenEntityTypes = get("hidden_entity_types", Defaults.HIDDEN_ENTITY_TYPES, new TypeToken<List<EntityType<?>>>(){}.getType());
        whitelistedPlayers = get("whitelisted_players", Defaults.WHITELISTED_PLAYERS, new TypeToken<List<String>>(){}.getType());
        eventTypes = get("notifications", Defaults.EVENT_TYPES, new TypeToken<List<EventType>>(){}.getType());

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

        // Update version
        set("version", Versions.EU_VERSION);
        save();
    }

    private void update(@NotNull String oldKey, @NotNull String newKey, @NotNull Type type) {
        set(newKey, get(oldKey, type));
        remove(oldKey);
    }

    // Make sure these are all mutable!
    // List.of() -> new ArrayList<>(List.of()), Set.of() -> new HashSet<>(Set.of()), etc...
    public static class Defaults {
        public static final boolean DISCORD_RPC = true;
        public static final boolean AUTO_TP = false;
        public static final boolean SIMPLE_QUEUE_MESSAGE = false;
        public static final boolean UPDATE_CHECKER = true;
        public static final boolean CONFIRM_WINDOW_CLOSE = true;
        public static final boolean CONFIRM_DISCONNECT = true;
        public static final int HIDE_PLAYERS_RADIUS = -1;
        @NotNull public static final String DEFAULT_FAMOUS_IP = "play.invadedlands.net";
        @NotNull public static final List<EntityType<?>> HIDDEN_ENTITY_TYPES = new ArrayList<>(List.of(EntityType.GLOW_ITEM_FRAME));
        @NotNull public static final List<String> HIDDEN_ENTITY_TYPES_STRING = new ArrayList<>(List.of("minecraft:glow_item_frame"));
        @NotNull public static final List<String> WHITELISTED_PLAYERS = new ArrayList<>(List.of("skeppy", "badboyhalo"));
        @NotNull public static final List<EventType> EVENT_TYPES = new ArrayList<>(List.of(EventType.values()));
    }
}
