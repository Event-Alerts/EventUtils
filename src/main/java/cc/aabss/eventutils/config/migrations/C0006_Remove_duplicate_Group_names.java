package cc.aabss.eventutils.config.migrations;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class C0006_Remove_duplicate_Group_names extends NamedMigration {
    public C0006_Remove_duplicate_Group_names() {
        super("removes Groups with duplicate names", (config, view) -> {
            // Get groups
            final Map<UUID, Group> groups = view.get("groups", GenericsDeclaration.of(Map.class, List.of(UUID.class, Group.class)));
            if (groups == null) return false;

            // Remove groups with duplicate names
            boolean updated = false;
            final Set<String> names = new HashSet<>();
            for (final Group group : groups.values()) {
                if (names.add(group.getName().toLowerCase())) continue;
                EventUtils.LOGGER.error("Removing duplicate group: {}", group);
                groups.remove(group.getUuid());
                updated = true;
            }
            if (updated) {
                view.set("groups", groups);
                return true;
            }

            return false;
        });
    }
}
