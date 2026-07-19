package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.EUConfig;
import cc.aabss.eventutils.screen.group.GroupManagerScreen;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.apache.logging.log4j.spi.StandardLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.text.Text.translatable;


public class ConfigScreen {
    @NotNull
    public static Screen getConfigScreen(@Nullable Screen parent) {
        final EUConfig config = EventUtils.MOD.config;
        final YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
            .title(translatable("eventutils.config.title"))
            .category(ConfigCategory.createBuilder().name(translatable("eventutils.config.general"))
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.teleport.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.teleport.description")))
                            .binding(EUConfig.Defaults.AUTO_TP, () -> config.autoTp, newValue -> {
                                config.autoTp = newValue;
                                config.setSave("auto_tp", config.autoTp);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.queue.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.queue.description")))
                            .binding(EUConfig.Defaults.SIMPLE_QUEUE_MESSAGE, () -> config.simpleQueueMessage, newValue -> {
                                config.simpleQueueMessage = newValue;
                                config.setSave("simple_queue_message", config.simpleQueueMessage);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.update.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.update.description")))
                            .binding(EUConfig.Defaults.UPDATE_CHECKER, () -> config.updateChecker, newValue -> {
                                config.updateChecker = newValue;
                                config.setSave("update_checker", config.updateChecker);
                                EventUtils.MOD.updateChecker.notifyUpdate();
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.window.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.window.description")))
                            .binding(EUConfig.Defaults.CONFIRM_WINDOW_CLOSE, () -> config.confirmWindowClose, newValue -> {
                                config.confirmWindowClose = newValue;
                                config.setSave("confirm_window_close", config.confirmWindowClose);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.disconnect.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.disconnect.description")))
                            .binding(EUConfig.Defaults.CONFIRM_DISCONNECT, () -> config.confirmDisconnect, newValue -> {
                                config.confirmDisconnect = newValue;
                                config.setSave("confirm_disconnect", config.confirmDisconnect);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    .option(Option.<String>createBuilder()
                            .name(translatable("eventutils.config.famous.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.famous.description")))
                            .binding(EUConfig.Defaults.DEFAULT_FAMOUS_IP, () -> config.defaultFamousIp, newValue -> {
                                config.defaultFamousIp = newValue;
                                config.setSave("default_famous_ip", config.defaultFamousIp);
                            })
                            .controller(StringControllerBuilder::create).build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.bee_icons.title"))
                            .description(OptionDescription.of(translatable("eventutils.config.bee_icons.description")))
                            .binding(EUConfig.Defaults.BEE_ICONS, () -> config.beeIcons, newValue -> {
                                config.beeIcons = newValue;
                                config.setSave("bee_icons", config.beeIcons);
                            })
                            .controller(ConfigScreen::getBooleanBuilder).build())
                    // Advanced
                    .group(OptionGroup.createBuilder()
                            .name(translatable("eventutils.config.advanced"))
                            .collapsed(true)
                            .option(Option.<Boolean>createBuilder()
                                    .name(translatable("eventutils.config.advanced.developer_mode.title"))
                                    .description(OptionDescription.of(translatable("eventutils.config.advanced.developer_mode.description")))
                                    .binding(EUConfig.Defaults.DEVELOPER_MODE, () -> config.developerMode, newValue -> {
                                        config.developerMode = newValue;
                                        EventUtils.MOD.updateLogLevel();
                                        EventUtils.MOD.setupSdk("Developer Mode enabled/disabled");
                                        config.setSave("developer_mode", config.developerMode);
                                    })
                                    .controller(ConfigScreen::getBooleanBuilder).build())
                            .option(Option.<StandardLevel>createBuilder()
                                    .name(translatable("eventutils.config.advanced.log_level.title"))
                                    .description(OptionDescription.of(translatable("eventutils.config.advanced.log_level.description")))
                                    .binding(EUConfig.Defaults.LOG_LEVEL, () -> config.logLevel, newValue -> {
                                        config.logLevel = newValue;
                                        EventUtils.MOD.updateLogLevel();
                                        config.setSave("log_level", config.logLevel);
                                    })
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(StandardLevel.class)).build())
                            .build())
                    .build());

        // Alerts & notification sounds
        final OptionGroup.Builder alertsGroup = OptionGroup.createBuilder()
                .name(translatable("eventutils.config.alerts.toggles"));
        final OptionGroup.Builder serverListGroup = OptionGroup.createBuilder()
                .name(translatable("eventutils.config.alerts.server_list"));
        final OptionGroup.Builder soundsGroup = OptionGroup.createBuilder()
                .name(translatable("eventutils.config.alerts.sounds"))
                .collapsed(true);

        serverListGroup.option(Option.<Boolean>createBuilder()
                .name(translatable("eventutils.config.server_list_enabled.title"))
                .description(OptionDescription.of(translatable("eventutils.config.server_list_enabled.description")))
                .binding(EUConfig.Defaults.EVENT_SERVERS_ENABLED, () -> config.eventServersEnabled, newValue -> {
                    config.eventServersEnabled = newValue;
                    config.setSave("event_servers_enabled", config.eventServersEnabled);
                })
                .controller(ConfigScreen::getBooleanBuilder).build());
        serverListGroup.option(Option.<Integer>createBuilder()
                .name(translatable("eventutils.config.server_list_minutes.title"))
                .description(OptionDescription.of(translatable("eventutils.config.server_list_minutes.description")))
                .binding(EUConfig.Defaults.EVENT_SERVER_DISPLAY_MINUTES, () -> config.eventServerDisplayMinutes, newValue -> {
                    config.setEventServerDisplayMinutes(newValue);
                    config.setSave("event_server_display_minutes", config.eventServerDisplayMinutes);
                })
                .controller(option -> IntegerFieldControllerBuilder.create(option).min(1)).build());

        for (final EventType type : EventType.values()) {
            alertsGroup.option(type.getOption(config));
            serverListGroup.option(type.getServerListOption(config));
            soundsGroup.option(type.getSoundOption(config));
        }
        final ConfigCategory.Builder alertsCategory = ConfigCategory.createBuilder()
                .name(translatable("eventutils.config.alerts"));
        alertsCategory.group(alertsGroup.build());
        alertsCategory.group(serverListGroup.build());
        alertsCategory.group(soundsGroup.build());
        builder.category(alertsCategory.build());

        // Groups
        builder.category(ConfigCategory.createBuilder().name(translatable("eventutils.config.groups.category"))
                .option(ButtonOption.createBuilder()
                        .name(translatable("eventutils.config.groups.manage_title"))
                        .description(OptionDescription.of(translatable("eventutils.config.groups.manage_description")))
                        .action((yaclScreen, option) -> MinecraftClient.getInstance().setScreen(new GroupManagerScreen(yaclScreen, EventUtils.MOD)))
                        .build())
                .build());

        // Return
        return builder.build().generateScreen(parent);
    }

    @NotNull
    public static BooleanControllerBuilder getBooleanBuilder(@NotNull Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).coloured(true).onOffFormatter();
    }
}
