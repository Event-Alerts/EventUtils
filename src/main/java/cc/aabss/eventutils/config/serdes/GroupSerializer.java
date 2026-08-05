package cc.aabss.eventutils.config.serdes;

import cc.aabss.eventutils.config.Group;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;


public class GroupSerializer implements ObjectSerializer<Group> {
    @Override
    public boolean supports(@NotNull Class<?> type) {
        return Group.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NotNull Group object, @NotNull SerializationData data, @NotNull GenericsDeclaration generics) {
        data.set("name", object.getName());
        data.set("players", object.getPlayers());
        data.set("entities", object.getEntities());
        data.set("player_mode", object.getPlayerMode());
        data.set("entity_mode", object.getEntityMode());
        data.set("npc_mode", object.getNpcMode());
        data.set("radius", object.getRadius());
    }

    @Override @NotNull
    public Group deserialize(@NotNull DeserializationData data, @NotNull GenericsDeclaration generics) {
        final Group group = new Group();
        group.setName(data.get("name", String.class));
        group.setPlayerMode(data.getOr("player_mode", Group.Mode.class, Group.Defaults.PLAYER_MODE));
        group.setEntityMode(data.getOr("entity_mode", Group.Mode.class, Group.Defaults.ENTITY_MODE));
        group.setNpcMode(data.getOr("npc_mode", Group.Mode.class, Group.Defaults.NPC_MODE));
        group.setRadius(data.getOr("radius", Integer.class, Group.Defaults.RADIUS));

        // players
        Set<String> players = data.get("players", GenericsDeclaration.of(Set.class, List.of(String.class)));
        if (players == null) players = Group.Defaults.players();
        group.setPlayers(players);

        // entities
        Set<EntityType<?>> entities = data.get("entities", GenericsDeclaration.of(Set.class, List.of(EntityType.class)));
        if (entities == null) entities = Group.Defaults.entities();
        group.setEntities(entities);

        return group;
    }
}
