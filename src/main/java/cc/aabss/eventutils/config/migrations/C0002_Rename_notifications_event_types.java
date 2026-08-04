package cc.aabss.eventutils.config.migrations;

import cc.aabss.eventutils.config.EventType;
import eu.okaeri.configs.migrate.builtin.NamedMigration;

import java.util.HashSet;
import java.util.Set;


public class C0002_Rename_notifications_event_types extends NamedMigration {
    public C0002_Rename_notifications_event_types() {
        super("renames event types in notifications (older than 2.0.0)", (config, view) -> {
            final Set<EventType> types = new HashSet<>();
            for (final EventType type : EventType.values()) {
                if (Boolean.TRUE.equals(view.get(type.name().toLowerCase().replace("_", "-") + "-event", Boolean.class))) {
                    types.add(type);
                }
            }
            if (!types.isEmpty()) {
                view.set("notifications", types);
                return true;
            }
            return false;
        });
    }
}
