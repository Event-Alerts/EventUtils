package cc.aabss.eventutils.config;

import org.jetbrains.annotations.NotNull;


public class EventSettings {
    public boolean toasts = Defaults.TOASTS;
    @NotNull public NotificationSound sound = Defaults.SOUND;
    public boolean infoScreen = Defaults.INFO_SCREEN;
    public boolean autoTp = Defaults.AUTO_TP;
    public boolean serverList = Defaults.SERVER_LIST;

    public static class Defaults {
        public static final boolean TOASTS = true;
        public static final NotificationSound SOUND = NotificationSound.ALERT;
        public static final boolean INFO_SCREEN = true;
        public static final boolean AUTO_TP = false;
        public static final boolean SERVER_LIST = true;
    }
}
