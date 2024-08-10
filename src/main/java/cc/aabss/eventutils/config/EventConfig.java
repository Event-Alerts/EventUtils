package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.Versions;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;

import net.minecraft.entity.EntityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class EventConfig {
    @NotNull private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(EntityType.class, new EntityTypeAdapter())
            .registerTypeAdapter(new TypeToken<List<EntityType<?>>>(){}.getType(), new EntityTypeListAdapter())
            .registerTypeAdapter(new TypeToken<List<EventType>>(){}.getType(), new EventTypeListAdapter()).create();

    @NotNull private final File file;
    @NotNull private JsonObject json = new JsonObject();

    public boolean discordRpc;
    public boolean autoTp;
    public boolean simpleQueueMsg;
    public boolean updateChecker;
    public boolean confirmWindowClose;
    public boolean confirmDisconnect;
    @NotNull public String defaultFamousIp;
    @NotNull public List<EntityType<?>> hiddenEntityTypes;
    @NotNull public List<String> whitelistedPlayers;
    @NotNull public final Set<EventType> eventTypes;

    public EventConfig() {
        file = new File(FabricLoader.getInstance().getConfigDir().toString(), "eventutils.json");

        if (!file.exists()) {
            // Create default file if it doesn't exist
            createDefault();
        } else try (final FileReader fileReader = new FileReader(file)) {
            // Load existing file
            json = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (final IOException | JsonParseException e) {
            save(); // Save empty config if loading fails
        }

        // Check for updates
        update();

        // Get values
        discordRpc = get("discord_rpc", true);
        autoTp = get("auto_tp", false);
        simpleQueueMsg = get("simple_queue_message", false);
        updateChecker = get("update_checker", true);
        confirmWindowClose = get("confirm_window_close", true);
        confirmDisconnect = get("confirm_disconnect", true);
        defaultFamousIp = get("default_famous_ip", "play.invadedlands.net");
        hiddenEntityTypes = get("hidden_entity_types", List.of(EntityType.GLOW_ITEM_FRAME), new TypeToken<List<EntityType<?>>>(){}.getType());
        whitelistedPlayers = get("whitelisted_players", List.of("skeppy", "badboyhalo"), new TypeToken<List<String>>(){}.getType());
        eventTypes = get("notifications", Set.of(EventType.values()), new TypeToken<Set<EventType>>(){}.getType());
    }

    private void createDefault() {
        // Get default file
        final InputStream resource = getClass().getResourceAsStream("/config.json");
        if (resource == null) throw new RuntimeException("Failed to get resource file");

        // Load default file
        try (final InputStreamReader fileReader = new InputStreamReader(resource)) {
            json = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (final IOException | JsonParseException e) {
            throw new RuntimeException("Failed to load resource file", e);
        }

        // Save default file to config file
        save();
    }

    @Nullable
    public JsonElement get(@NotNull String key) {
        return json.get(key);
    }

    @NotNull
    public <T> T get(@NotNull String key, @NotNull T defaultValue) {
        return get(key, defaultValue, TypeToken.of(defaultValue.getClass()).getType());
    }

    @NotNull
    public <T> T get(@NotNull String key, @NotNull T defaultValue, @NotNull Type typeOfT) {
        final JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            setSave(key, defaultValue);
            return defaultValue;
        }
        try {
            return GSON.fromJson(element, typeOfT);
        } catch (final JsonSyntaxException | IllegalStateException e) {
            setSave(key, defaultValue);
            e.printStackTrace();
            return defaultValue;
        }
    }

    public <T> void set(@NotNull String key, @Nullable T value) {
        if (value == null) {
            json.remove(key);
            return;
        }
        json.add(key, GSON.toJsonTree(value));
    }

    public <T> void setSave(@NotNull String key, @Nullable T value) {
        set(key, value);
        save();
    }

    public void remove(@NotNull String key) {
        json.remove(key);
    }

    public void save() {
        try (final FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(GSON.toJson(json));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    private void update() {
        // Get old version
        final String oldVersionString = get("version", "1.4.0");
        final SemanticVersion oldVersion = Versions.getSemantic(oldVersionString);
        if (oldVersion == null) {
            EventUtils.LOGGER.error("Failed to parse config version: " + oldVersionString);
            return;
        }

        // Older than 2.0.0
        if (oldVersion.compareTo((Version) new SemanticVersionImpl(new int[]{2, 0, 0}, null, null)) < 0) {
            update("discord-rpc", "discord_rpc", true);
            update("auto-tp", "auto_tp", false);
            update("simple-queue-msg", "simple_queue_message", false);
            update("update-checker", "update_checker", true);
            update("confirm-window-close", "confirm_window_close", true);
            update("confirm-disconnect", "confirm_disconnect", true);
            update("default-famous-ip", "default_famous_ip", "play.invadedlands.net");

            // whitelisted_players
            set("whitelisted_players", get("whitelisted-players", List.of("skeppy", "badboyhalo"), new TypeToken<List<String>>(){}.getType()).stream()
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

    private <T> void update(@NotNull String oldKey, @NotNull String newKey, @NotNull T def) {
        set(newKey, get(oldKey, def));
        remove(oldKey);
    }
}
