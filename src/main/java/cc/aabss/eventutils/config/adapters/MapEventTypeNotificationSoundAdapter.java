package cc.aabss.eventutils.config.adapters;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.config.NotificationSound;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MapEventTypeNotificationSoundAdapter extends TypeAdapter<Map<EventType, NotificationSound>> {
    @Override
    public void write(@NotNull JsonWriter out, @NotNull Map<EventType, NotificationSound> value) throws IOException {
        out.beginObject();
        for (final Map.Entry<EventType, NotificationSound> entry : value.entrySet()) {
            out.name(entry.getKey().name());
            out.value(entry.getValue().name());
        }
        out.endObject();
    }

    @Override
    @NotNull
    public Map<EventType, NotificationSound> read(@NotNull JsonReader in) throws IOException {
        final Map<EventType, NotificationSound> map = new HashMap<>();
        in.beginObject();
        while (in.hasNext()) {
            final String name = in.nextName();
            final EventType type = EventType.fromString(name);
            if (type == null) {
                in.skipValue();
                continue;
            }
            final NotificationSound value = NotificationSound.fromString(in.nextString());
            if (value == null) continue;
            map.put(type, value);
        }
        in.endObject();
        return map;
    }
}
