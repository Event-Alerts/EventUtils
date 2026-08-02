package cc.aabss.eventutils.config.adapters;

import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.config.NotificationSound;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import xyz.srnyx.javautilities.manipulation.Mapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


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
            final Optional<EventType> type = Mapper.toEnum(in.nextName(), EventType.class);
            if (type.isEmpty()) {
                in.skipValue();
                continue;
            }
            Mapper.toEnum(in.nextString(), NotificationSound.class).ifPresent(value -> map.put(type.get(), value));
        }
        in.endObject();
        return map;
    }
}
