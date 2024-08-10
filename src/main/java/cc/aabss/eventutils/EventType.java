package cc.aabss.eventutils;

import cc.aabss.eventutils.config.ConfigScreen;
import cc.aabss.eventutils.config.EventConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


public enum EventType {
    FAMOUS("Famous Events", Text.literal("New Famous Event!").formatted(Formatting.AQUA)),
    POTENTIAL_FAMOUS("Potential Famous Events", Text.literal("New Potential Famous Event!").formatted(Formatting.DARK_AQUA)),
    PARTNER("Partner Events", Text.literal("New Partner Event!").formatted(Formatting.LIGHT_PURPLE)),
    COMMUNITY("Community Events", Text.literal("New Community Event!").formatted(Formatting.DARK_GRAY)),
    MONEY("Money Events", prize -> Text.literal("New Money Event! ").formatted(Formatting.GREEN).append("($" + prize + ")").formatted(Formatting.GRAY)),
    FUN("Fun Events", Text.literal("New Fun Event!").formatted(Formatting.RED)),
    HOUSING("Housing Events", Text.literal("New Housing Event!").formatted(Formatting.GOLD)),
    CIVILIZATION("Civilization Events", Text.literal("New Civilization Event!").formatted(Formatting.BLUE));

    @NotNull private static final Map<Long, EventType> FROM_ROLE_ID = Map.of(
            970434201990070424L, PARTNER,
            980950599946362900L, COMMUNITY,
            970434305203511359L, MONEY,
            970434303391576164L, FUN,
            970434294893928498L, HOUSING,
            1134932175821734119L, CIVILIZATION);

    @NotNull public final String displayName;
    @NotNull public final Function<Integer, MutableText> toast;

    EventType(@NotNull String displayName, @NotNull Function<Integer, MutableText> toast) {
        this.displayName = displayName;
        this.toast = toast;
    }

    EventType(@NotNull String displayName, @NotNull MutableText toast) {
        this(displayName, prize -> toast);
    }

    @NotNull
    public Option<Boolean> getOption(@NotNull EventConfig config) {
        return Option.<Boolean>createBuilder()
                .name(Text.literal(displayName))
                .description(OptionDescription.of(Text.literal("Whether you should be pinged for " + displayName.toLowerCase())))
                .binding(true, () -> config.eventTypes.contains(this), newValue -> {
                    if (Boolean.TRUE.equals(newValue)) {
                        config.eventTypes.add(this);
                    } else {
                        config.eventTypes.remove(this);
                    }
                    config.setSave("notifications", config.eventTypes);
                })
                .controller(ConfigScreen::getBooleanBuilder)
                .build();
    }

    public void sendToast(@Nullable Integer prize) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        final MutableText description = Text.literal("Type ").formatted(Formatting.WHITE)
                .append("/eventtp " + name().toLowerCase()).formatted(Formatting.YELLOW)
                .append(" to teleport!").formatted(Formatting.WHITE);
        client.getToastManager().add(new NotificationToast(toast.apply(prize), description));
        if (client.player != null) client.player.playSound(SoundEvent.of(Identifier.of("eventutils", "alert")), 1 ,1);
    }

    @Nullable
    public static EventType fromString(@NotNull String eventType) {
        try {
            return EventType.valueOf(eventType.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static EventType fromRoleId(long roleId) {
        return FROM_ROLE_ID.get(roleId);
    }

    @NotNull
    public static Set<EventType> fromJson(@NotNull JsonObject json) {
        final Set<EventType> eventTypes = new HashSet<>();
        final JsonArray roles = json.getAsJsonArray("roles");
        if (roles == null) return eventTypes;
        for (final JsonElement role : roles) {
            final EventType eventType = fromRoleId(role.getAsLong());
            if (eventType != null) eventTypes.add(eventType);
        }
        return eventTypes;
    }
}
