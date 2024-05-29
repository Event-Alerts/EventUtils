package cc.aabss.eventutils.config;

import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonConfig {

    private final File EventUtils;
    public JsonObject JSON;
    private final Gson GSON = new Gson();

    public JsonConfig(File config) {
        this.EventUtils = config;
        registerConfigs();
        loadJson();
    }

    // -----------

    public void registerConfigs() {
        try {
            if (new File(EventUtils.getAbsolutePath()).mkdirs() || EventUtils.createNewFile()) {
                JsonObject json = new JsonObject();
                try (FileWriter fileWriter = new FileWriter(EventUtils.getPath())) {
                    fileWriter.write(json.toString());
                } catch (IOException e) {
                    throw new IOException(e);
                }
                JSON = json;
            } else{
                loadJson();
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public void loadJson(){
        try (FileReader fileReader = new FileReader(EventUtils.getPath())) {
            JsonObject json;
            try {
                json = JsonParser.parseReader(fileReader).getAsJsonObject();
            } catch (JsonParseException ignored){
                json = new JsonObject();
            }
            saveConfig(json);
            JSON = json;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T>T loadObject(String key, T defaultValue) {
        JsonElement a = JSON.get(key);
        if (a == null) {
            saveObject(key, defaultValue);
            return defaultValue;
        } else {
            return GSON.fromJson(a, (Class<T>) defaultValue.getClass());
        }
    }

    public <T>T registerObject(String key, T defaultValue){
        return loadObject(key, defaultValue);
    }

    public void saveConfig(JsonObject json){
        try (FileWriter fileWriter = new FileWriter(EventUtils.getPath())) {
            fileWriter.write(json.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public <T> void saveObject(String key, T value) {
        JSON.add(key.toLowerCase().replaceAll(" ", "-").replaceAll("_", "-"), GSON.toJsonTree(value));
        saveConfig(JSON);
    }


}