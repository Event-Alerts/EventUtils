package cc.aabss.eventutils.sdk;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.NotificationToast;
import cc.aabss.eventutils.config.EventSettings;
import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.config.NotificationSound;
import cc.aabss.eventutils.utility.ConnectUtility;
import cc.aabss.eventutils.utility.MarkdownSanitizer;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.parents.Stringable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EventWrapper {
    @NotNull private final EventUtils mod;
    @NotNull public final Object event;
    @NotNull public final ObjectId id;
    /**
     * Sorted by {@link EventType#ordinal()} (0 = highest priority)
     */
    @NotNull public final List<EventType> eventTypes = new ArrayList<>();
    @NotNull public final String title;
    @Nullable public final String ip;
    @Nullable public final String prize;

    public EventWrapper(@NotNull EventUtils mod, @NotNull EAEvent event) {
        this.mod = mod;
        this.event = event;
        this.id = Objects.requireNonNullElse(event.id, new ObjectId());
        this.prize = extractPrize();

        // eventTypes
        if (event.rolesNamed != null) {
            for (final EAEvent.PingRole role : event.rolesNamed) {
                final EventType type = EventType.fromPingRole(role);
                if (type != null) eventTypes.add(type);
            }
            eventTypes.sort(Comparator.comparingInt(Enum::ordinal));
        }

        // title
        if (event.title != null) {
            this.title = event.title;
        } else if (!eventTypes.isEmpty()) {
            this.title = eventTypes.get(0).name() + " Event"; // don't use getFirst to support lower Java versions
        } else {
            this.title = "Unknown Event";
        }

        // ip
        if (event.rolesNamed != null && event.rolesNamed.contains(EAEvent.PingRole.HOUSING)) {
            this.ip = "hypixel.net";
        } else {
            this.ip = extractIp();
        }
    }

    public EventWrapper(@NotNull EventUtils mod, @NotNull EAFamousEvent event) {
        this.mod = mod;
        this.event = event;
        this.id = new ObjectId();
        this.prize = null;

        // eventTypes
        EventType eventType = EventType.fromFamousEventType(event.type);
        if (eventType == EventType.FAMOUS && event.channel != null && event.channel == 1006347642500022353L) eventType = EventType.SKEPPY;
        if (eventType != null) eventTypes.add(eventType);

        // title
        this.title = (eventType != null ? eventType.name() : "Unknown") + " Event";

        // ip
        this.ip = Objects.requireNonNullElse(ConnectUtility.getIp(event.message), mod.config.default_famous_ip).toLowerCase();
    }

    @Override @NotNull
    public String toString() {
        return Stringable.toString(this, "mod");
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

    public void executeTypeSettings() {
        boolean toastSent = false;
        boolean playedSound = false;
        boolean teleported = false;
        boolean addedToServerList = false;
        for (final EventType eventType : eventTypes) {
            mod.lastEvents.put(eventType, this);
            final EventSettings settings = mod.config.getEventSettings(eventType);

            // toast
            if (!toastSent && settings.toasts) {
                final MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    // Get title
                    final String prizeTranslation = prize != null ? "eventutils.event.toast.prize.text" : "eventutils.event.toast.prize.none";
                    final Text title = Text.translatable("eventutils.event.toast.title", eventType.nameTranslation, Text.translatable(prizeTranslation, prize)).formatted(eventType.color);

                    // Get description
                    final Text description = ip != null ? Text.translatable("eventutils.event.teleport.text", Text.translatable("eventutils.event.teleport.command", eventType.name().toLowerCase()).formatted(eventType.color)) : null;

                    // Update metric
                    mod.stats.eventToastsReceived.incrementAndGet();

                    // Send toast
                    client.execute(() -> client.getToastManager().add(new NotificationToast(title, description, ip != null)));
                    toastSent = true;
                }
            }

            // sound
            if (!playedSound && settings.sound != NotificationSound.NONE) {
                settings.sound.play();
                playedSound = true;
            }

            // infoScreen
            if (settings.infoScreen) mod.keybindManager.lastEventForInfoScreen = this;

            // IP-reliant stuff
            if (ip == null) continue;

            // autoTp
            if (!teleported && settings.autoTp) {
                ConnectUtility.connect(ip);
                teleported = true;
            }

            // serverList
            if (!addedToServerList && settings.serverList) try {
                mod.eventServerManager.addEventServer(this);
                addedToServerList = true;
            } catch (final Exception e) {
                EventUtils.LOGGER.warn("Failed to add event server to server list: {}", event, e);
            }
        }
    }

    @Nullable
    private String extractIp() {
        if (!(event instanceof EAEvent eaEvent)) return null;

        // Direct IP field
        if (eaEvent.ip != null && !eaEvent.ip.isEmpty()) return eaEvent.ip.toLowerCase();

        // Extract from description
        if (eaEvent.description != null) {
            final String extracted = ConnectUtility.getIp(eaEvent.description);
            if (extracted != null) return extracted.toLowerCase();
        }

        // Extract from title
        if (eaEvent.title != null) {
            final String extracted = ConnectUtility.getIp(eaEvent.title);
            if (extracted != null) return extracted.toLowerCase();
        }

        return null;
    }

    @NotNull private static final Pattern PRIZE_PATTERN = Pattern.compile(
            "(?<symbol>[$€£])\\s*(?<symbolAmount>\\d+(?:,\\d{3})*)|(?<wordAmount>\\d+(?:,\\d{3})*)\\s*(?<currency>dollars?|euros?|pounds?)",
            Pattern.CASE_INSENSITIVE);

    @Nullable
    private String extractPrize() {
        if (!(event instanceof EAEvent eaEvent) || eaEvent.rolesNamed == null || !eaEvent.rolesNamed.contains(EAEvent.PingRole.MONEY)) {
            return null;
        }

        // Prize from JSON
        if (eaEvent.prize != null) {
            final Matcher matcher = PRIZE_PATTERN.matcher(eaEvent.prize);
            if (matcher.find()) return formatPrize(matcher);
        }

        // Prize from description
        if (eaEvent.description != null) {
            for (final String line : MarkdownSanitizer.sanitize(eaEvent.description).split("\\n+")) {
                final Matcher matcher = PRIZE_PATTERN.matcher(line);
                if (matcher.find()) return formatPrize(matcher);
            }
        }

        return null;
    }

    @NotNull
    private static String formatPrize(@NotNull Matcher matcher) {
        if (matcher.group("symbol") != null) {
            return matcher.group("symbol") + matcher.group("symbolAmount").replace(",", "");
        }

        final String symbol = switch (matcher.group("currency").toLowerCase()) {
            case "dollar", "dollars" -> "$";
            case "euro", "euros" -> "€";
            case "pound", "pounds" -> "£";
            default -> "";
        };
        return symbol + matcher.group("wordAmount").replace(",", "");
    }
}
