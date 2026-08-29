package cc.aabss.eventutils.versioning;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
//? >=26.2 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.world.entity.EntityType;
//?}


public class VersionedEntityType {
    @Nullable
    public static <T extends Entity> EntityType<T> getEntityType(String name) {
        return (EntityType<T>)
                //? if >=26.2 {
                /*BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(name))
                *///?} else {
                EntityType.byString(name)
                //?}
                .orElse(null);
    }
}
