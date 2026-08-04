package cc.aabss.eventutils.config.serdes;

import cc.aabss.eventutils.config.EventSettings;
import cc.aabss.eventutils.config.NotificationSound;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.jetbrains.annotations.NotNull;


public class EventSettingsSerializer implements ObjectSerializer<EventSettings> {
    @Override
    public boolean supports(@NotNull Class<?> type) {
        return EventSettings.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NotNull EventSettings object, @NotNull SerializationData data, @NotNull GenericsDeclaration generics) {
        data.set("toasts", object.toasts);
        data.set("sound", object.sound);
        data.set("info_screen", object.infoScreen);
        data.set("auto_tp", object.autoTp);
        data.set("server_list", object.serverList);
    }

    @Override @NotNull
    public EventSettings deserialize(@NotNull DeserializationData data, @NotNull GenericsDeclaration generics) {
        final EventSettings settings = new EventSettings();
        settings.toasts = data.getOr("toasts", Boolean.class, EventSettings.Defaults.TOASTS);
        settings.sound = data.getOr("sound", NotificationSound.class, EventSettings.Defaults.SOUND);
        settings.infoScreen = data.getOr("info_screen", Boolean.class, EventSettings.Defaults.INFO_SCREEN);
        settings.autoTp = data.getOr("auto_tp", Boolean.class, EventSettings.Defaults.AUTO_TP);
        settings.serverList = data.getOr("server_list", Boolean.class, EventSettings.Defaults.SERVER_LIST);
        return settings;
    }
}
