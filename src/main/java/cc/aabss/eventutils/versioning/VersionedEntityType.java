package cc.aabss.eventutils.versioning;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? >=26.2 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
*///?}


public class VersionedEntityType {
    /**
     * @param   identifier  example: {@code minecraft:chicken}
     */
    @Nullable
    public static <T extends Entity> EntityType<T> getEntityTypeByIdentifier(@NotNull String identifier) {
        try {
            return (EntityType<T>)
                    //? if >=26.2 {
                    /*BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(identifier))
                    *///?} else {
                    EntityType.byString(identifier)
                     //?}
                    .orElse(null);
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * @param   name     example: {@code chicken}
     */
    @Nullable
    public static <T extends Entity> EntityType<T> getEntityTypeByName(@NotNull String name) {
        return getEntityTypeByIdentifier("minecraft:" + name);
    }
}
