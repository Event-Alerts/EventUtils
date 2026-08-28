package cc.aabss.eventutils.config;

import cc.aabss.eventutils.screen.config.ConfigScreen;
import cc.aabss.eventutils.versioning.VersionedIdentifier;import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.EnumDropdownControllerBuilder;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Ordering matters! Top = most "important"
 */
public enum EventType {
    // EAFamousEvent
    SKEPPY(EAFamousEvent.Type.SKEPPY, ChatFormatting.AQUA),
    FAMOUS(EAFamousEvent.Type.FAMOUS, ChatFormatting.AQUA),
    POTENTIAL_FAMOUS(EAFamousEvent.Type.POTENTIAL_FAMOUS, ChatFormatting.DARK_AQUA),
    SIGHTING(EAFamousEvent.Type.SIGHTING, ChatFormatting.DARK_RED),

    // EAEvent
    BIG_MONEY(EAEvent.PingRole.BIG_MONEY, ChatFormatting.DARK_GREEN),
    MONEY(EAEvent.PingRole.MONEY, ChatFormatting.GREEN),
    CIVILIZATION(EAEvent.PingRole.CIVILIZATION, ChatFormatting.BLUE),
    HOUSING(EAEvent.PingRole.HOUSING, ChatFormatting.GOLD),
    FUN(EAEvent.PingRole.FUN, ChatFormatting.RED),
    PARTNER(EAEvent.PingRole.PARTNER, ChatFormatting.LIGHT_PURPLE),
    COMMUNITY(EAEvent.PingRole.COMMUNITY, ChatFormatting.GRAY);


    @Nullable public final EAEvent.PingRole pingRole;
    @Nullable public final EAFamousEvent.Type famousEventType;
    @NotNull public final ChatFormatting color;
    @NotNull public final Component nameTranslation = Component.translatable("eventutils.event.type." + name());
    @NotNull public final Component descriptionTranslation = Component.translatable("eventutils.event.description." + name());

    EventType(@Nullable EAEvent.PingRole pingRole, @Nullable EAFamousEvent.Type famousEventType, @NotNull ChatFormatting color) {
        this.pingRole = pingRole;
        this.famousEventType = famousEventType;
        this.color = color;
    }

    EventType(@NotNull EAEvent.PingRole pingRole, @NotNull ChatFormatting color) {
        this(pingRole, null, color);
    }

    EventType(@NotNull EAFamousEvent.Type famousEventType, @NotNull ChatFormatting color) {
        this(null, famousEventType, color);
    }

    @NotNull
    public OptionGroup getOptionGroup(@NotNull EUConfig config) {
        return OptionGroup.createBuilder()
                .name(Component.translatable("eventutils.config.event_settings.title", nameTranslation).withStyle(color))
                .description(OptionDescription.of(Component.translatable("eventutils.config.event_settings.description", descriptionTranslation)))
                .collapsed(true)
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("eventutils.config.event_settings.toasts.label"))
                        .description(OptionDescription.createBuilder()
                                .text(Component.translatable("eventutils.config.event_settings.toasts.description"))
                                .image(VersionedIdentifier.of("textures/config/toast.png"), 767, 128)
                                .build())
                        .binding(EventSettings.Defaults.TOASTS, () -> config.getEventSettings(this).toasts, newValue -> {
                            config.getEventSettingsOrCreate(this).toasts = newValue;
                            config.save();
                        })
                        .controller(ConfigScreen::getBooleanBuilder)
                        .build())
                .option(Option.<NotificationSound>createBuilder()
                        .name(Component.translatable("eventutils.config.event_settings.sound.label"))
                        .description(OptionDescription.of(Component.translatable("eventutils.config.event_settings.sound.description")))
                        .binding(EventSettings.Defaults.SOUND, () -> config.getEventSettings(this).sound, newValue -> {
                            config.getEventSettingsOrCreate(this).sound = newValue;
                            config.save();
                        })
                        .controller(opt -> EnumDropdownControllerBuilder.create(opt)
                                .formatValue(NotificationSound::getDisplayName))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("eventutils.config.event_settings.info_screen.label"))
                        .description(OptionDescription.createBuilder()
                                .text(Component.translatable("eventutils.config.event_settings.info_screen.description"))
                                .image(VersionedIdentifier.of("textures/config/info_screen.png"), 1106, 898)
                                .build())
                        .binding(EventSettings.Defaults.INFO_SCREEN, () -> config.getEventSettings(this).infoScreen, newValue -> {
                            config.getEventSettingsOrCreate(this).infoScreen = newValue;
                            config.save();
                        })
                        .controller(ConfigScreen::getBooleanBuilder)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("eventutils.config.event_settings.auto_tp.label"))
                        .description(OptionDescription.of(Component.translatable("eventutils.config.event_settings.auto_tp.description")))
                        .binding(EventSettings.Defaults.AUTO_TP, () -> config.getEventSettings(this).autoTp, newValue -> {
                            config.getEventSettingsOrCreate(this).autoTp = newValue;
                            config.save();
                        })
                        .controller(ConfigScreen::getBooleanBuilder)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("eventutils.config.event_settings.server_list.label"))
                        .description(OptionDescription.createBuilder()
                                .text(Component.translatable("eventutils.config.event_settings.server_list.description"))
                                .image(VersionedIdentifier.of("textures/config/server_listing.png"), 975, 415)
                                .build())
                        .binding(EventSettings.Defaults.SERVER_LIST, () -> config.getEventSettings(this).serverList, newValue -> {
                            config.getEventSettingsOrCreate(this).serverList = newValue;
                            config.save();
                        })
                        .controller(ConfigScreen::getBooleanBuilder)
                        .build())
                .build();
    }

    @Nullable
    public static EventType fromPingRole(@NotNull EAEvent.PingRole pingRole) {
        for (final EventType eventType : values()) if (eventType.pingRole == pingRole) return eventType;
        return null;
    }

    @Nullable
    public static EventType fromFamousEventType(@Nullable EAFamousEvent.Type famousEventType) {
        if (famousEventType == null) return null;
        for (final EventType eventType : values()) if (eventType.famousEventType == famousEventType) return eventType;
        return null;
    }
}
