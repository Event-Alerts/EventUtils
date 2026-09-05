package cc.aabss.eventutils.config.serdes;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.config.Groups;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class GroupsSerializer implements ObjectSerializer<Groups> {
    @Override
    public boolean supports(@NotNull Class<?> type) {
        return Groups.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NotNull Groups object, @NotNull SerializationData data, @NotNull GenericsDeclaration generics) {
        data.setValue(object.toMap(), GenericsDeclaration.of(Map.class, List.of(UUID.class, Group.class)));
    }

    @Override @NotNull
    public Groups deserialize(@NotNull DeserializationData data, @NotNull GenericsDeclaration generics) {
        final Map<UUID, Group> map = data.getValueDirect(GenericsDeclaration.of(Map.class, List.of(UUID.class, Group.class)));

        // Add UUIDs to Groups
        for (final Map.Entry<UUID, Group> entry : map.entrySet()) entry.getValue().setUuid(entry.getKey());

        // Validate (unique names)
        final Set<String> groupNames = new HashSet<>();
        for (final Group group : map.values()) {
            if (groupNames.add(group.getName().toLowerCase())) continue;
            EventUtils.LOGGER.error("Removing duplicate group: {}", group);
            map.remove(group.getUuid());
        }

        return new Groups(map);
    }
}
