package cc.aabss.eventutils.config.adapters;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class EntityTypeListAdapter extends TypeAdapter<List<EntityType<?>>> {
    @Override
    public void write(@NotNull JsonWriter out, @NotNull List<EntityType<?>> value) throws IOException {
        out.beginArray();
        for (final EntityType<?> entityType : value) out.value(EntityType.getId(entityType).toString());
        out.endArray();
    }

    @Override @NotNull
    public List<EntityType<?>> read(@NotNull JsonReader in) throws IOException {
        final List<EntityType<?>> entityTypes = new ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
            final String id = in.nextString();
            final Optional<EntityType<?>> entityType = EntityType.get(id);
            if (entityType.isEmpty()) throw new JsonParseException("Unknown entity type: " + id);
            entityTypes.add(entityType.get());
        }
        in.endArray();
        return entityTypes;
    }
}
