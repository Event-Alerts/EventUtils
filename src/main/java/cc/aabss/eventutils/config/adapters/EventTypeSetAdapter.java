package cc.aabss.eventutils.config.adapters;

import cc.aabss.eventutils.EventType;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;


public class EventTypeSetAdapter extends TypeAdapter<Set<EventType>> {
    @Override
    public void write(@NotNull JsonWriter out, @NotNull Set<EventType> value) throws IOException {
        out.beginArray();
        for (final EventType type : value) out.value(type.name());
        out.endArray();
    }

    @Override @NotNull
    public Set<EventType> read(@NotNull JsonReader in) throws IOException {
        final Set<EventType> eventTypes = new HashSet<>();
        in.beginArray();
        while (in.hasNext()) {
            final EventType type = EventType.fromString(in.nextString());
            if (type != null) eventTypes.add(type);
        }
        in.endArray();
        return eventTypes;
    }
}
