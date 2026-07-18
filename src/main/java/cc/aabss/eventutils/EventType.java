package cc.aabss.eventutils;

import cc.aabss.eventutils.screen.ConfigScreen;
import cc.aabss.eventutils.config.EventConfig;
import cc.aabss.eventutils.config.NotificationSound;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.controller.EnumDropdownControllerBuilder;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static net.minecraft.text.Text.translatable;


public enum EventType {
    SKEPPY(EAFamousEvent.Type.SKEPPY, "eventutils.skeppy.display", translatable("eventutils.skeppy.new").formatted(Formatting.AQUA)),
    POTENTIAL_FAMOUS(EAFamousEvent.Type.POTENTIAL_FAMOUS, "eventutils.potential_famous.display", translatable("eventutils.potential_famous.new").formatted(Formatting.DARK_AQUA)),
    SIGHTING(EAFamousEvent.Type.SIGHTING, "eventutils.sighting.display", translatable("eventutils.sighting.new").formatted(Formatting.DARK_RED)),
    FAMOUS(EAFamousEvent.Type.FAMOUS, "eventutils.famous.display", translatable("eventutils.famous.new").formatted(Formatting.AQUA)),
    PARTNER(EAEvent.PingRole.PARTNER, "eventutils.partner.display", translatable("eventutils.partner.new").formatted(Formatting.LIGHT_PURPLE)),
    COMMUNITY(EAEvent.PingRole.COMMUNITY, "eventutils.community.display", translatable("eventutils.community.new").formatted(Formatting.GRAY)),
    MONEY(EAEvent.PingRole.MONEY, "eventutils.money.display", prize -> {
        final MutableText text = translatable("eventutils.money.new").formatted(Formatting.GREEN);
        if (prize != null && prize > 0) text.append(Text.literal(" ($" + prize + ")").formatted(Formatting.GRAY));
        return text;
    }),
    FUN(EAEvent.PingRole.FUN, "eventutils.fun.display", translatable("eventutils.fun.new").formatted(Formatting.RED)),
    HOUSING(EAEvent.PingRole.HOUSING, "eventutils.housing.display", translatable("eventutils.housing.new").formatted(Formatting.GOLD)),
    CIVILIZATION(EAEvent.PingRole.CIVILIZATION, "eventutils.civilization.display", translatable("eventutils.civilization.new").formatted(Formatting.BLUE));

    @Nullable public final EAEvent.PingRole pingRole;
    @Nullable public final EAFamousEvent.Type famousEventType;
    @NotNull public final MutableText displayName;
    @NotNull public final String displayNameString;
    @NotNull public final Function<Integer, MutableText> toast;

    EventType(@Nullable EAEvent.PingRole pingRole, @Nullable EAFamousEvent.Type famousEventType, @NotNull String translateKey, @NotNull Function<Integer, MutableText> toast) {
        this.pingRole = pingRole;
        this.famousEventType = famousEventType;
        this.displayName = translatable(translateKey);
        this.displayNameString = this.name().toLowerCase().replace("_", "");
        this.toast = toast;
    }

    EventType(@NotNull EAEvent.PingRole pingRole, @NotNull String translateKey, @NotNull Function<Integer, MutableText> toast) {
        this(pingRole, null, translateKey, toast);
    }

    EventType(@NotNull EAEvent.PingRole pingRole, @NotNull String displayName, @NotNull MutableText toast) {
        this(pingRole, displayName, prize -> toast);
    }

    EventType(@NotNull EAFamousEvent.Type famousEventType, @NotNull String displayName, @NotNull MutableText toast) {
        this(null, famousEventType, displayName, prize -> toast);
    }

    @NotNull
    public Option<Boolean> getOption(@NotNull EventConfig config) {
        return Option.<Boolean>createBuilder()
                .name(displayName)
                .description(OptionDescription.of(Text.of(EventUtils.translate("eventutils.config.event_description").replace("{event}", displayName.getString()))))
                .binding(true, () -> config.eventTypes.contains(this), newValue -> {
                    if (newValue) {
                        config.eventTypes.add(this);
                    } else {
                        config.eventTypes.remove(this);
                    }
                    config.setSave("notifications", config.eventTypes);
                })
                .controller(ConfigScreen::getBooleanBuilder)
                .build();
    }

    @NotNull
    public Option<NotificationSound> getSoundOption(@NotNull EventConfig config) {
        return Option.<NotificationSound>createBuilder()
                .name(displayName)
                .description(OptionDescription.of(Text.of(EventUtils.translate("eventutils.config.sound_description").replace("{event}", displayName.getString()))))
                .binding(
                        EventConfig.Defaults.notificationSounds().get(this),
                        () -> config.notificationSounds.get(this),
                        newValue -> {
                            config.notificationSounds.put(this, newValue);
                            config.setSave("notification_sounds", config.notificationSounds);
                        })
                .controller(EnumDropdownControllerBuilder::create)
                .addListener((option, event) -> {
                    if (event == OptionEventListener.Event.STATE_CHANGE) option.pendingValue().play();
                })
                .build();
    }

    @NotNull
    public Option<Boolean> getServerListOption(@NotNull EventConfig config) {
        return Option.<Boolean>createBuilder()
                .name(displayName)
                .description(OptionDescription.of(Text.of(EventUtils.translate("eventutils.config.server_list_event_description").replace("{event}", displayName.getString()))))
                .binding(true, () -> config.eventServerTypes.contains(this), newValue -> {
                    if (newValue) {
                        if (!config.eventServerTypes.contains(this)) config.eventServerTypes.add(this);
                    } else {
                        config.eventServerTypes.remove(this);
                    }
                    config.setSave("event_server_types", config.eventServerTypes);
                })
                .controller(ConfigScreen::getBooleanBuilder)
                .build();
    }

    public void sendToast(@NotNull EventUtils mod, @Nullable Integer prize, boolean hasIp) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Get toast description
        MutableText description = null;
        if (hasIp) {
            final String[] split = EventUtils.translate("eventutils.event.teleport").split("\\{command}");
            description = Text.literal(split[0]).formatted(Formatting.WHITE)
                    .append("/eventutils teleport " + name().toLowerCase()).formatted(Formatting.YELLOW)
                    .append(split[1]).formatted(Formatting.WHITE);
        }

        // Send toast and play sound
        client.getToastManager().add(new NotificationToast(toast.apply(prize), description, client.player != null));
        mod.config.getNotificationSound(this).play();
    }

    @Nullable
    public static EventType fromString(@NotNull String eventType) {
        try {
            return EventType.valueOf(eventType.toUpperCase());
        } catch (final IllegalArgumentException e) {
            EventUtils.LOGGER.warn("Invalid event type: {}", eventType);
            return null;
        }
    }

    @Nullable
    public static EventType fromPingRole(@NotNull EAEvent.PingRole pingRole) {
        for (final EventType eventType : values()) if (eventType.pingRole == pingRole) return eventType;
        return null;
    }

    @Nullable
    public static EventType fromFamousEventType(@NotNull EAFamousEvent.Type famousEventType) {
        for (final EventType eventType : values()) if (eventType.famousEventType == famousEventType) return eventType;
        return null;
    }
}
