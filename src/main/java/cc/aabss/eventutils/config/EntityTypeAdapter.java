package cc.aabss.eventutils.config;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

class EntityTypeAdapter extends TypeAdapter<EntityType<?>> {
    @Override
    public void write(@NotNull JsonWriter out, @NotNull EntityType<?> value) throws IOException {
        out.value(EntityType.getId(value).toString());
    }

    @Override
    @NotNull
    public EntityType<?> read(@NotNull JsonReader in) throws IOException {
        final String id = in.nextString();
        final Optional<EntityType<?>> entityType = EntityType.get(id);
        if (entityType.isEmpty()) throw new JsonParseException("Unknown entity type: " + id);
        return entityType.get();
    }
}
