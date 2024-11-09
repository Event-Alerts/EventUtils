package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.text.Text.translatable;


public class ConfigScreen {
    @NotNull private static final List<String> ENTITY_TYPES = Registries.ENTITY_TYPE.stream()
            .map(entityType -> EntityType.getId(entityType).toString())
            .toList();

    @NotNull
    public static Screen getConfigScreen(@Nullable Screen parent) {
        final EventUtils mod = EventUtils.MOD;
        final EventConfig config = mod.config;
        return YetAnotherConfigLib.createBuilder()
            .title(translatable("eventutils.config.title"))
            .category(ConfigCategory.createBuilder().name(translatable("eventutils.config.alerts"))
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.teleport.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.teleport.description")))
                            .binding(EventConfig.Defaults.AUTO_TP, () -> config.autoTp, newValue -> {
                                config.autoTp = newValue;
                                config.setSave("auto_tp", config.autoTp);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.queue.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.queue.description")))
                            .binding(EventConfig.Defaults.SIMPLE_QUEUE_MESSAGE, () -> config.simpleQueueMessage, newValue -> {
                                config.simpleQueueMessage = newValue;
                                config.setSave("simple_queue_message", config.simpleQueueMessage);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.discord.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.discord.description")))
                            .binding(EventConfig.Defaults.DISCORD_RPC, () -> config.discordRpc, newValue -> {
                                config.discordRpc = newValue;
                                config.setSave("discord_rpc", config.discordRpc);
                                if (Boolean.TRUE.equals(newValue)) {
                                    mod.discordRPC.connect();
                                } else {
                                    mod.discordRPC.disconnect();
                                }
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.update.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.update.description")))
                            .binding(EventConfig.Defaults.UPDATE_CHECKER, () -> config.updateChecker, newValue -> {
                                config.updateChecker = newValue;
                                config.setSave("update_checker", config.updateChecker);
                                if (Boolean.TRUE.equals(newValue)) mod.updateChecker.checkUpdate();
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.window.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.window.description")))
                            .binding(EventConfig.Defaults.CONFIRM_WINDOW_CLOSE, () -> config.confirmWindowClose, newValue -> {
                                config.confirmWindowClose = newValue;
                                config.setSave("confirm_window_close", config.confirmWindowClose);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.disconnect.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.disconnect.description")))
                            .binding(EventConfig.Defaults.CONFIRM_DISCONNECT, () -> config.confirmDisconnect, newValue -> {
                                config.confirmDisconnect = newValue;
                                config.setSave("confirm_disconnect", config.confirmDisconnect);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<String>createBuilder()
                            .name(translatable("eventutils.config.famous.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.famous.description")))
                            .binding(EventConfig.Defaults.DEFAULT_FAMOUS_IP, () -> config.defaultFamousIp, newValue -> {
                                config.defaultFamousIp = newValue;
                                config.setSave("default_famous_ip", config.defaultFamousIp);
                            })
                            .controller(StringControllerBuilder::create).build())
                    .option(Option.<Integer>createBuilder()
                            .name(translatable("eventutils.config.radius.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.radius.description")))
                            .binding(EventConfig.Defaults.HIDE_PLAYERS_RADIUS, () -> config.hidePlayersRadius, newValue -> {
                                config.hidePlayersRadius = newValue;
                                config.setSave("hide_players_radius", config.hidePlayersRadius);
                            })
                            .controller((option) -> IntegerSliderControllerBuilder.create(option).range(1, 255)).build())
                    .group(ListOption.<String>createBuilder()
                            .name(translatable("eventutils.config.entity.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.entity.description")))
                            .binding(EventConfig.Defaults.HIDDEN_ENTITY_TYPES_STRING, () -> config.hiddenEntityTypes.stream()
                                            .map(entityType -> EntityType.getId(entityType).toString())
                                            .toList(),
                                    newValue -> {
                                        config.hiddenEntityTypes = newValue.stream()
                                                .map(id -> EntityType.get(id).orElse(null))
                                                .collect(Collectors.toList());
                                        config.setSave("hidden_entity_types", config.hiddenEntityTypes);
                                    })
                            .controller(option -> DropdownStringControllerBuilder.create(option).values(ENTITY_TYPES))
                            .initial(EntityType.getId(EntityType.ALLAY).toString()).build())
                    .group(ListOption.<String>createBuilder()
                            .name(translatable("eventutils.config.players.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.players.description")))
                            .binding(EventConfig.Defaults.WHITELISTED_PLAYERS, () -> new ArrayList<>(config.whitelistedPlayers), newValue -> {
                                config.whitelistedPlayers = newValue.stream()
                                        .map(String::toLowerCase)
                                        .toList();
                                config.setSave("whitelisted_players", config.whitelistedPlayers);
                            })
                            .controller(StringControllerBuilder::create)
                            .initial("skeppy").build())
                    .build())
            .category(ConfigCategory.createBuilder().name(translatable("eventutils.config.alerts"))
                    .option(EventType.FAMOUS.getOption(config))
                    .option(EventType.POTENTIAL_FAMOUS.getOption(config))
                    .option(EventType.SIGHTING.getOption(config))
                    .option(EventType.PARTNER.getOption(config))
                    .option(EventType.COMMUNITY.getOption(config))
                    .option(EventType.MONEY.getOption(config))
                    .option(EventType.FUN.getOption(config))
                    .option(EventType.HOUSING.getOption(config))
                    .option(EventType.CIVILIZATION.getOption(config)).build())
                .build().generateScreen(parent);
    }

    @NotNull
    public static BooleanControllerBuilder getBooleanBuilder(@NotNull Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).coloured(true).onOffFormatter();
    }
}
