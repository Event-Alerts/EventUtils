package cc.aabss.eventutils.config.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;


public class CollectionAdapter<T, C extends Collection<T>> extends TypeAdapter<C> {
    @NotNull private final Supplier<@NotNull C> supplier;
    @NotNull private final Serializer<T> serializer;
    @NotNull private final Deserializer<T> deserializer;

    public CollectionAdapter(@NotNull Supplier<@NotNull C> supplier, @NotNull Serializer<T> serializer, @NotNull Deserializer<T> deserializer) {
        this.supplier = supplier;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public void write(@NotNull JsonWriter out, @NotNull C value) throws IOException {
        out.beginArray();
        for (final T element : value) out.value(serializer.serialize(element));
        out.endArray();
    }

    @Override @NotNull
    public C read(@NotNull JsonReader in) throws IOException {
        final C collection = supplier.get();
        in.beginArray();
        while (in.hasNext()){
            final T element = deserializer.deserialize(in.nextString());
            if (element != null) collection.add(element);
        }
        in.endArray();
        return collection;
    }

    @FunctionalInterface
    public static interface Serializer<T> {
        @NotNull
        String serialize(@NotNull T value);
    }

    @FunctionalInterface
    public static interface Deserializer<T> {
        @Nullable
        T deserialize(@NotNull String value);
    }
}
