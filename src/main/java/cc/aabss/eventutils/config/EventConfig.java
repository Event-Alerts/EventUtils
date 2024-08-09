package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.entity.EntityType;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;


public class EventConfig {
    @NotNull private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(EntityType.class, new EntityTypeAdapter())
            .registerTypeAdapter(new TypeToken<List<EntityType<?>>>(){}.getType(), new EntityTypeListAdapter())
            .registerTypeAdapter(new TypeToken<List<EventType>>(){}.getType(), new EventTypeListAdapter()).create();

    @NotNull private final File file;
    @NotNull public JsonObject json = new JsonObject();
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

        discordRpc = get("discord-rpc", true);
        autoTp = get("auto-tp", false);
        simpleQueueMsg = get("simple-queue-msg", false);
        updateChecker = get("update-checker", true);
        confirmWindowClose = get("confirm-window-close", true);
        confirmDisconnect = get("confirm-disconnect", true);
        defaultFamousIp = get("default-famous-ip", "play.invadedlands.net");
        hiddenEntityTypes = get("hidden-entity-types", List.of(EntityType.GLOW_ITEM_FRAME), new TypeToken<List<EntityType<?>>>(){}.getType());
        whitelistedPlayers = get("whitelisted-players", List.of("skeppy", "badboyhalo"), new TypeToken<List<String>>(){}.getType());
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

    public <T> T get(@NotNull String key, @NotNull T defaultValue) {
        return get(key, defaultValue, new com.google.gson.reflect.TypeToken<T>(){}.getType());
    }

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

    public <T> void set(@NotNull String key, @NotNull T value) {
        json.add(key, GSON.toJsonTree(value));
    }

    public <T> void setSave(@NotNull String key, @NotNull T value) {
        set(key, value);
        save();
    }

    public void save() {
        try (final FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(GSON.toJson(json));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }
}
