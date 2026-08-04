package cc.aabss.eventutils.config.serdes;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class EntityTypeSerializer extends BidirectionalTransformer<String, EntityType> {
    @Override @NotNull
    public GenericsPair<String, EntityType> getPair() {
        return genericsPair(String.class, EntityType.class);
    }

    @Override @Nullable
    public EntityType leftToRight(@NotNull String data, @NotNull SerdesContext serdesContext) {
        return EntityType.get(data).orElse(null);
    }

    @Override @NotNull
    public String rightToLeft(@NotNull EntityType data, @NotNull SerdesContext serdesContext) {
        return EntityType.getId(data).toString();
    }
}
