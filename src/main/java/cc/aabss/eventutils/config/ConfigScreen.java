package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;

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
        final EventConfig config = EventUtils.MOD.config;
        final YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
            .title(translatable("eventutils.config.title"))
            .category(ConfigCategory.createBuilder().name(translatable("eventutils.config.general"))
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
                                    EventUtils.MOD.discordRPC.connect();
                                } else {
                                    EventUtils.MOD.discordRPC.disconnect();
                                }
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.update.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.update.description")))
                            .binding(EventConfig.Defaults.UPDATE_CHECKER, () -> config.updateChecker, newValue -> {
                                config.updateChecker = newValue;
                                config.setSave("update_checker", config.updateChecker);
                                if (Boolean.TRUE.equals(newValue)) EventUtils.MOD.updateChecker.checkUpdate();
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
                            .controller(option -> IntegerFieldControllerBuilder.create(option).min(0)).build())
                    .group(ListOption.<String>createBuilder()
                            .name(translatable("eventutils.config.entity.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.entity.description")))
                            .binding(EventConfig.Defaults.hiddenEntityTypesString(), () -> config.hiddenEntityTypes.stream()
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
                            .binding(EventConfig.Defaults.whitelistedPlayers(), () -> new ArrayList<>(config.whitelistedPlayers), newValue -> {
                                config.whitelistedPlayers = newValue.stream()
                                        .map(String::toLowerCase)
                                        .toList();
                                config.setSave("whitelisted_players", config.whitelistedPlayers);
                            })
                            .controller(StringControllerBuilder::create)
                            .initial("skeppy").build())
                    .build());

        // Alerts & notification sounds
        final OptionGroup.Builder alertsGroup = OptionGroup.createBuilder()
                .name(translatable("eventutils.config.alerts.toggles"));
        final OptionGroup.Builder soundsGroup = OptionGroup.createBuilder()
                .name(translatable("eventutils.config.alerts.sounds"))
                .collapsed(true);
        for (final EventType type : EventType.values()) {
            alertsGroup.option(type.getOption(config));
            soundsGroup.option(type.getSoundOption(config));
        }
        final ConfigCategory.Builder alertsCategory = ConfigCategory.createBuilder()
                .name(translatable("eventutils.config.alerts"));
        alertsCategory.group(alertsGroup.build());
        alertsCategory.group(soundsGroup.build());
        builder.category(alertsCategory.build());

        // Return
        return builder.build().generateScreen(parent);
    }

    @NotNull
    public static BooleanControllerBuilder getBooleanBuilder(@NotNull Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).coloured(true).onOffFormatter();
    }
}
