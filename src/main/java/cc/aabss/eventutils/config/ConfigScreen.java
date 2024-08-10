package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ConfigScreen {
    @NotNull private static final List<String> ENTITY_TYPES = Registries.ENTITY_TYPE.stream()
            .map(entityType -> EntityType.getId(entityType).toString())
            .toList();

    @NotNull
    public static YetAnotherConfigLib.Builder getConfigScreen(@NotNull EventUtils mod) {
        final EventConfig config = mod.config;
        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("EventUtils Mod Config"))
            .category(ConfigCategory.createBuilder().name(Text.literal("General"))
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Auto Teleport"))
                            .description(OptionDescription.of(Text.literal("Automatically teleports you to the server when an event starts")))
                            .binding(false, () -> config.autoTp, newValue -> {
                                config.autoTp = newValue;
                                config.setSave("auto-tp", config.autoTp);
                            })
                            .controller(option -> getBooleanBuilder(option).yesNoFormatter()).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Simplify Queue Message"))
                            .description(OptionDescription.of(Text.literal("Simplifies the queue message for InvadedLands")))
                            .binding(false, () -> config.simpleQueueMsg, newValue -> {
                                config.simpleQueueMsg = newValue;
                                config.setSave("simple-queue-msg", config.simpleQueueMsg);
                            })
                            .controller(option -> getBooleanBuilder(option).yesNoFormatter()).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Discord RPC"))
                            .description(OptionDescription.of(Text.literal("Whether the Discord rich presence should be shown")))
                            .binding(true, () -> config.discordRpc, newValue -> {
                                config.discordRpc = newValue;
                                config.setSave("discord-rpc", config.discordRpc);
                                if (Boolean.TRUE.equals(newValue)) {
                                    mod.discordRPC.connect();
                                } else {
                                    mod.discordRPC.disconnect();
                                }
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Mod Update Checker"))
                            .description(OptionDescription.of(Text.literal("Whether the mod should check and notify for updates")))
                            .binding(true, () -> config.updateChecker, newValue -> {
                                config.updateChecker = newValue;
                                config.setSave("update-checker", config.updateChecker);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Confirm Window Close"))
                            .description(OptionDescription.of(Text.literal("Whether a confirmation should pop up confirming you want to close your game window")))
                            .binding(true, () -> config.confirmWindowClose, newValue -> {
                                config.confirmWindowClose = newValue;
                                config.setSave("confirm-window-close", config.confirmWindowClose);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Confirm Disconnect"))
                            .description(OptionDescription.of(Text.literal("Whether a confirmation should pop up confirming you want to leave your server")))
                            .binding(true, () -> config.confirmDisconnect, newValue -> {
                                config.confirmDisconnect = newValue;
                                config.setSave("confirm-disconnect", config.confirmDisconnect);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<String>createBuilder()
                            .name(Text.literal("Default Famous IP"))
                            .description(OptionDescription.of(Text.literal("The default ip for if a [potential] famous event is pinged with no ip inputted")))
                            .binding("play.invadedlands.net", () -> config.defaultFamousIp, newValue -> {
                                config.defaultFamousIp = newValue;
                                config.setSave("default-famous-ip", config.defaultFamousIp);
                            })
                            .controller(StringControllerBuilder::create).build())
                    .option(ListOption.<String>createBuilder()
                            .name(Text.literal("Hidden Entity Types"))
                            .description(OptionDescription.of(Text.literal("The types of entities that will be hidden")))
                            .binding(List.of("minecraft:glow_item_frame"),
                                    () -> config.hiddenEntityTypes.stream()
                                            .map(entityType -> EntityType.getId(entityType).toString())
                                            .toList(),
                                    newValue -> {
                                        config.hiddenEntityTypes = newValue.stream()
                                                .map(id -> EntityType.get(id).orElse(null))
                                                .collect(Collectors.toList());
                                        config.setSave("hidden-entity-types", config.hiddenEntityTypes);
                                    })
                            .controller(option -> DropdownStringControllerBuilder.create(option).values(ENTITY_TYPES))
                            .initial(EntityType.getId(EntityType.ALLAY).toString()).build())
                    .option(ListOption.<String>createBuilder()
                            .name(Text.literal("Whitelisted Players"))
                            .description(OptionDescription.of(Text.literal("The names of the players you can see when players are hidden")))
                            .binding(List.of("Skeppy", "BadBoyHalo"), () -> new ArrayList<>(config.whitelistedPlayers), newValue -> {
                                config.whitelistedPlayers = newValue.stream()
                                        .map(String::toLowerCase)
                                        .toList();
                                config.setSave("whitelisted-players", config.whitelistedPlayers);
                            })
                            .controller(StringControllerBuilder::create)
                            .initial("Skeppy").build()).build())
            .category(ConfigCategory.createBuilder().name(Text.literal("Alerts"))
                    .option(EventType.FAMOUS.getOption(config))
                    .option(EventType.POTENTIAL_FAMOUS.getOption(config))
                    .option(EventType.PARTNER.getOption(config))
                    .option(EventType.COMMUNITY.getOption(config))
                    .option(EventType.MONEY.getOption(config))
                    .option(EventType.FUN.getOption(config))
                    .option(EventType.HOUSING.getOption(config))
                    .option(EventType.CIVILIZATION.getOption(config)).build());
    }

    @NotNull
    public static BooleanControllerBuilder getBooleanBuilder(@NotNull Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).coloured(true);
    }
}
