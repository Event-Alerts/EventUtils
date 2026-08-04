package cc.aabss.eventutils.config.migrations;

import cc.aabss.eventutils.config.EventSettings;
import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.config.NotificationSound;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;


public class C0003_Flat_alerts_to_EventSettings extends NamedMigration {
    public C0003_Flat_alerts_to_EventSettings() {
        super("migrates flat event alert settings to EventSettings (2.3.0 or older)",
                when(
                        any(
                                exists("auto_tp"),
                                exists("notifications"),
                                exists("notification_sounds"),
                                exists("event_servers_enabled"),
                                exists("event_server_types")),

                        ((config, view) -> {
                            // Get legacy settings
                            final Boolean autoTp = view.get("auto_tp", Boolean.class);
                            final Set<EventType> notifications = view.get("notifications", GenericsDeclaration.of(Set.class, List.of(EventType.class)));
                            final Map<EventType, NotificationSound> notificationSounds = view.get("notification_sounds", GenericsDeclaration.of(Map.class, List.of(EventType.class, NotificationSound.class)));
                            final Boolean eventServersEnabledGlobal = view.get("event_servers_enabled", Boolean.class);
                            final Set<EventType> eventServerTypes = view.get("event_server_types", GenericsDeclaration.of(Set.class, List.of(EventType.class)));

                            // Create EventSettings for each EventType
                            Map<EventType, Object> eventSettings = view.get("event_settings", GenericsDeclaration.of(Map.class, List.of(EventType.class, Object.class)));
                            if (eventSettings == null) eventSettings = new EnumMap<>(EventType.class);
                            for (final EventType type : EventType.values()) {
                                final EventSettings settings = new EventSettings();
                                if (autoTp != null) settings.autoTp = autoTp;
                                if (notifications != null) settings.toasts = notifications.contains(type);
                                if (notificationSounds != null) settings.sound = notificationSounds.getOrDefault(type, NotificationSound.ALERT);
                                if (eventServersEnabledGlobal != null && !eventServersEnabledGlobal) {
                                    // Global disabled -> all disabled
                                    settings.serverList = false;
                                } else if (eventServerTypes != null) {
                                    // Individual setting
                                    settings.serverList = eventServerTypes.contains(type);
                                }
                                eventSettings.put(type, settings);
                            }
                            view.set("event_settings", eventSettings);

                            // Remove legacy settings
                            view.remove("auto_tp");
                            view.remove("notifications");
                            view.remove("notification_sounds");
                            view.remove("event_servers_enabled");
                            view.remove("event_server_types");
                            return true;
                        })));
    }
}
