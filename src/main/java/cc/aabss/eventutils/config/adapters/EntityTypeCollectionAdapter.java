package cc.aabss.eventutils.config.adapters;

import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;


public class EntityTypeCollectionAdapter<C extends Collection<EntityType<?>>> extends CollectionAdapter<EntityType<?>, C> {
    public EntityTypeCollectionAdapter(@NotNull Supplier<C> supplier) {
        super(supplier, entityType -> EntityType.getId(entityType).toString(), string -> EntityType.get(string).orElse(null));
    }
}
