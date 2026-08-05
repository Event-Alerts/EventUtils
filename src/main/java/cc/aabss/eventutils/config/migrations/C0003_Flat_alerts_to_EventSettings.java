package cc.aabss.eventutils.config.migrations;

import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.config.NotificationSound;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;


public class C0003_Flat_alerts_to_EventSettings extends NamedMigration {
    // We want new EventTypes to use default values
    @NotNull private static final Set<EventType> TO_MIGRATE = Set.of(
            EventType.SKEPPY,
            EventType.FAMOUS,
            EventType.POTENTIAL_FAMOUS,
            EventType.SIGHTING,
            EventType.MONEY,
            EventType.CIVILIZATION,
            EventType.HOUSING,
            EventType.FUN,
            EventType.PARTNER,
            EventType.COMMUNITY);

    public C0003_Flat_alerts_to_EventSettings() {
        super("migrates flat event alert settings to EventSettings (2.3.0 or older)",
                when(
                        any(
                                exists("auto_tp"),
                                exists("notifications"),
                                exists("notification_sounds"),
                                exists("event_servers_enabled"),
                                exists("event_server_types")),

                        (config, view) -> {
                            // Get legacy settings
                            final boolean autoTp = view.getOr("auto_tp", Boolean.class, false);
                            final Set<EventType> notifications = view.get("notifications", GenericsDeclaration.of(Set.class, List.of(EventType.class)));
                            final Map<EventType, NotificationSound> notificationSounds = view.get("notification_sounds", GenericsDeclaration.of(Map.class, List.of(EventType.class, NotificationSound.class)));
                            final Boolean eventServersEnabledGlobal = view.get("event_servers_enabled", Boolean.class);
                            final Set<EventType> eventServerTypes = view.get("event_server_types", GenericsDeclaration.of(Set.class, List.of(EventType.class)));

                            // Create settings for each EventType
                            for (final EventType type : TO_MIGRATE) {
                                view.set("event_settings." + type + ".auto_tp", autoTp, Boolean.class);
                                view.set("event_settings." + type + ".toasts", notifications != null && notifications.contains(type), Boolean.class);
                                view.set("event_settings." + type + ".sound", notificationSounds != null ? notificationSounds.getOrDefault(type, NotificationSound.ALERT) : NotificationSound.ALERT, NotificationSound.class);
                                view.set("event_settings." + type + ".server_list", eventServersEnabledGlobal == null || eventServerTypes == null || (eventServersEnabledGlobal && eventServerTypes.contains(type)), Boolean.class);
                            }

                            // Remove legacy settings
                            view.remove("auto_tp");
                            view.remove("notifications");
                            view.remove("notification_sounds");
                            view.remove("event_servers_enabled");
                            view.remove("event_server_types");
                            return true;
                        }));
    }
}
