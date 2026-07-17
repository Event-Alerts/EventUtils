package cc.aabss.eventutils.sdk;

import gg.eventalerts.sdk.object.EAEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public class EventUtility {
    public static class PlatformUtility {
        @NotNull
        public static String toDisplayString(@Nullable Set<EAEvent.Platform> platforms) {
            if (platforms == null) return "";
            final StringBuilder builder = new StringBuilder();
            for (final EAEvent.Platform platform : platforms) builder.append(platform.displayName).append("/");
            if (!builder.isEmpty()) builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }
}
