package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import net.minecraft.entity.EntityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;


/**
 * srnyx made this class for an idea with handling config defaults but ended up scrapping it
 * <br>Going to keep this though as we may want to load multiple JSON files in the future maybe?
 */
public abstract class FileLoader {
    @NotNull private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(EntityType.class, new EntityTypeAdapter())
            .registerTypeAdapter(new TypeToken<List<EntityType<?>>>(){}.getType(), new EntityTypeListAdapter())
            .registerTypeAdapter(new TypeToken<List<EventType>>(){}.getType(), new EventTypeListAdapter()).create();

    @NotNull protected final File file;
    @NotNull protected JsonObject json = new JsonObject();

    public FileLoader(@NotNull File file) {
        this.file = file;
    }

    protected void load(@NotNull File newFile) {
        try (final FileReader fileReader = new FileReader(newFile)) {
            json = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (final IOException | JsonParseException e) {
            json = new JsonObject();
            save(); // Save empty config if loading fails
        }
    }

    protected void load() {
        load(file);
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
    public <T> T get(@NotNull String key, @NotNull T defaultValue, @NotNull Type type) {
        final T value = get(key, type);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public <T> T get(@NotNull String key, @NotNull Type type) {
        final JsonElement element = json.get(key);
        if (element == null) {
            setSave(key, null);
            return null;
        }
        try {
            return GSON.fromJson(element, type);
        } catch (final JsonSyntaxException | IllegalStateException e) {
            setSave(key, null);
            e.printStackTrace();
            return null;
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
}
