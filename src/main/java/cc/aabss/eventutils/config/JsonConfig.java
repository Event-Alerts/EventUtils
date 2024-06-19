package cc.aabss.eventutils.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.entity.EntityType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class JsonConfig {

    private final File EventUtils;
    public JsonObject JSON;
    private final Gson GSON;

    public JsonConfig(File config) {
        this.EventUtils = config;
        this.GSON = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(EntityType.class, new EntityTypeAdapter())
                .create();
        registerConfigs();
    }

    public void registerConfigs() {
        try {
            if (EventUtils.createNewFile()) {
                JSON = new JsonObject();
                saveConfig(JSON);
            } else {
                loadJson();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadJson() {
        try (FileReader fileReader = new FileReader(EventUtils)) {
            JSON = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            JSON = new JsonObject();
        }
    }

    public <T> T loadObject(String key, T defaultValue) {
        return loadObject(key, defaultValue, defaultValue.getClass());
    }

    public <T> T loadObject(String key, T defaultValue, Type typeOfT) {
        JsonElement element = JSON.get(key);
        if (element == null || element.isJsonNull()) {
            saveObject(key, defaultValue);
            return defaultValue;
        } else {
            try {
                return GSON.fromJson(element, typeOfT);
            } catch (JsonSyntaxException e) {
                saveObject(key, defaultValue);
                return defaultValue;
            }
        }
    }

    public void saveConfig(JsonObject json) {
        try (FileWriter fileWriter = new FileWriter(EventUtils)) {
            fileWriter.write(GSON.toJson(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void saveObject(String key, T value) {
        JSON.add(key.toLowerCase().replace(" ", "-").replace("_", "-"), GSON.toJsonTree(value));
        saveConfig(JSON);
    }

    private static class EntityTypeAdapter extends TypeAdapter<EntityType<?>> {

        @Override
        public void write(JsonWriter out, EntityType<?> value) throws IOException {
            out.value(EntityType.getId(value).toString());
        }

        @Override
        public EntityType<?> read(JsonReader in) throws IOException {
            String entityId = in.nextString();
            for (Field field : EntityType.class.getFields()) {
                try {
                    Object object = field.get(null);
                    if (object instanceof EntityType<?> entityType) {
                        if (EntityType.getId(entityType).toString().equals(entityId)) {
                            return entityType;
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new IOException(e);
                }
            }
            throw new JsonParseException("Unknown entity type: " + entityId);
        }
    }
}
