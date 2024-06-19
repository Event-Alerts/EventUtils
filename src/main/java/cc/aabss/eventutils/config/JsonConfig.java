package cc.aabss.eventutils.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.entity.EntityType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonConfig {

    private final File EventUtils;
    public JsonObject JSON;
    private final Gson GSON;

    public JsonConfig(File config) {
        this.EventUtils = config;
        this.GSON = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(EntityType.class, new EntityTypeAdapter())
                .registerTypeAdapter(new TypeToken<List<EntityType<?>>>() {}.getType(), new EntityTypeListAdapter())
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
            throw new RuntimeException("Failed to create or load config file", e);
        }
    }

    public void loadJson() {
        try (FileReader fileReader = new FileReader(EventUtils)) {
            JSON = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            JSON = new JsonObject();
            saveConfig(JSON); // Save the empty config if loading fails
        }
    }

    public <T> T loadObject(String key, T defaultValue) {
        return loadObject(key, defaultValue, new TypeToken<T>(){}.getType());
    }

    public <T> T loadObject(String key, T defaultValue, Type typeOfT) {
        JsonElement element = JSON.get(key);
        if (element == null || element.isJsonNull()) {
            saveObject(key, defaultValue);
            return defaultValue;
        } else {
            try {
                return GSON.fromJson(element, typeOfT);
            } catch (JsonSyntaxException | IllegalStateException e) {
                saveObject(key, defaultValue);
                System.out.println("Thrown exception: " + e);
                return defaultValue;
            }
        }
    }

    public void saveConfig(JsonObject json) {
        try (FileWriter fileWriter = new FileWriter(EventUtils)) {
            fileWriter.write(GSON.toJson(json));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    public <T> void saveObject(String key, T value) {
        JSON.add(key, GSON.toJsonTree(value));
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

    private static class EntityTypeListAdapter extends TypeAdapter<List<EntityType<?>>> {
        @Override
        public void write(JsonWriter out, List<EntityType<?>> value) throws IOException {
            out.beginArray();
            for (EntityType<?> entityType : value) {
                out.value(EntityType.getId(entityType).toString());
            }
            out.endArray();
        }

        @Override
        public List<EntityType<?>> read(JsonReader in) throws IOException {
            List<EntityType<?>> entityTypes = new ArrayList<>();
            in.beginArray();
            while (in.hasNext()) {
                String entityId = in.nextString();
                boolean found = false;
                for (Field field : EntityType.class.getFields()) {
                    try {
                        Object object = field.get(null);
                        if (object instanceof EntityType<?> entityType) {
                            if (EntityType.getId(entityType).toString().equals(entityId)) {
                                entityTypes.add(entityType);
                                found = true;
                                break;
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new IOException(e);
                    }
                }
                if (!found) {
                    throw new JsonParseException("Unknown entity type: " + entityId);
                }
            }
            in.endArray();
            return entityTypes;
        }
    }
}
