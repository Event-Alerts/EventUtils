package cc.aabss.eventutils.config.serdes;

import cc.aabss.eventutils.config.Group;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.jetbrains.annotations.NotNull;

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
        group.setName(data.getOr("name", String.class, group.getUuid().toString()));
        group.setPlayers(data.getOr("players", Set.class, Group.Defaults.players())); //TODO type works?
        group.setEntities(data.getOr("entities", Set.class, Group.Defaults.entities())); //TODO type works?
        group.setPlayerMode(data.getOr("player_mode", Group.Mode.class, Group.Defaults.PLAYER_MODE));
        group.setEntityMode(data.getOr("entity_mode", Group.Mode.class, Group.Defaults.ENTITY_MODE));
        group.setNpcMode(data.getOr("npc_mode", Group.Mode.class, Group.Defaults.NPC_MODE));
        group.setRadius(data.getOr("radius", Integer.class, Group.Defaults.RADIUS));
        return group;
    }
}
