package cc.aabss.eventutils.sdk;

import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class EventWrapper {
    @NotNull private final Object event;

    public EventWrapper(@NotNull EAEvent event) {
        this.event = event;
    }

    public EventWrapper(@NotNull EAFamousEvent event) {
        this.event = event;
    }

    public boolean isEAEvent() {
        return event instanceof EAEvent;
    }

    public boolean isEAFamousEvent() {
        return event instanceof EAFamousEvent;
    }

    @NotNull
    public EAEvent asEAEvent() {
        return (EAEvent) event;
    }

    @NotNull
    public EAFamousEvent asEAFamousEvent() {
        return (EAFamousEvent) event;
    }

    @NotNull
    public List<String> toInfoScreenText() {
        final List<String> lines = new ArrayList<>();

        // EAEvent
        if (isEAEvent()) {
            final EAEvent eaEvent = asEAEvent();
            if (eaEvent.title != null) lines.add(eaEvent.title);
            if (eaEvent.host != null) lines.add("Host: " + eaEvent.host);
            if (eaEvent.server != null) lines.add("Partner Server: " + eaEvent.server);
            if (eaEvent.created != null) lines.add("Created: " + formatTime(eaEvent.created));
            if (eaEvent.time != null) lines.add("Time: " + formatTime(eaEvent.time));
            if (eaEvent.ip != null) lines.add("IP: " + eaEvent.ip);
            if (eaEvent.prize != null) lines.add("Prize: " + eaEvent.prize);

            // Version
            final StringBuilder version = new StringBuilder();
            if (eaEvent.platforms != null && !eaEvent.platforms.isEmpty()) version.append(EventUtility.PlatformUtility.toDisplayString(eaEvent.platforms));
            if (eaEvent.version != null) {
                if (!version.isEmpty()) version.append(" ");
                version.append(eaEvent.version);
            }
            if (!version.isEmpty()) lines.add("Version: " + version);

            // Roles
            if (eaEvent.rolesNamed != null && !eaEvent.rolesNamed.isEmpty()) lines.add("Roles: " + eaEvent.rolesNamed.stream()
                    .map(role -> role.displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));

            if (eaEvent.description != null) lines.add("Description: " + eaEvent.description);
            if (eaEvent.id != null) lines.add("ID: " + eaEvent.id);

            return lines;
        }

        // EAFamousEvent
        if (isEAFamousEvent()) {
            final EAFamousEvent famousEvent = asEAFamousEvent();
            if (famousEvent.type != null) lines.add("Type: " + famousEvent.type);
            if (famousEvent.user != null) lines.add("User: " + famousEvent.user);
            if (famousEvent.message != null) lines.add("Message: " + famousEvent.message);
            return lines;
        }

        lines.add("Unknown event type: " + event.getClass().getName());
        return lines;
    }

    @NotNull
    private static String formatTime(@NotNull Date date) {
        Duration duration = Duration.between(Instant.now(), date.toInstant());
        final boolean future = !duration.isNegative();
        duration = duration.abs();

        final long hours = duration.toHours();
        final long minutes = duration.toMinutes();
        final long seconds = duration.toSecondsPart();
        return (future ? "in " : "") + hours + "h " + minutes + "m " + seconds + "s" + (future ? "" : " ago");
    }
}
