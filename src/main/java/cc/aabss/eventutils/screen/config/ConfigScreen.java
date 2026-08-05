package cc.aabss.eventutils.screen.config;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.EUConfig;
import cc.aabss.eventutils.screen.config.group.GroupManagerScreen;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
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
                            .name(translatable("eventutils.config.discord.label"))
                            .description(OptionDescription.createBuilder()
                                    .text(translatable("eventutils.config.discord.description"))
                                    .image(Identifier.of(BuildProperties.MOD_ID, "textures/config/discord_rpc.png"), 351, 165)
                                    .build())
                            .binding(EUConfig.Defaults.DISCORD_RPC, () -> config.discord_rpc, newValue -> {
                                config.discord_rpc = newValue;
                                config.save();
                                EventUtils.MOD.discordRPC.refreshConnection();
                            })
                            .controller(ConfigScreen::getBooleanBuilder)
                            .build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.queue.label"))
                            .description(OptionDescription.of(translatable("eventutils.config.queue.description")))
                            .binding(EUConfig.Defaults.SIMPLE_QUEUE_MESSAGE, () -> config.simple_queue_message, newValue -> {
                                config.simple_queue_message = newValue;
                                config.save();
                            })
                            .controller(ConfigScreen::getBooleanBuilder)
                            .build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.update.label"))
                            .description(OptionDescription.of(translatable("eventutils.config.update.description")))
                            .binding(EUConfig.Defaults.UPDATE_CHECKER, () -> config.update_checker, newValue -> {
                                config.update_checker = newValue;
                                config.save();
                                EventUtils.MOD.updateChecker.notifyUpdate();
                            })
                            .controller(ConfigScreen::getBooleanBuilder)
                            .build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.window.label"))
                            .description(OptionDescription.createBuilder()
                                    .text(translatable("eventutils.config.window.description"))
                                    .image(Identifier.of(BuildProperties.MOD_ID, "textures/config/confirm_exit.png"), 950, 272)
                                    .build())
                            .binding(EUConfig.Defaults.CONFIRM_WINDOW_CLOSE, () -> config.confirm_window_close, newValue -> {
                                config.confirm_window_close = newValue;
                                config.save();
                            })
                            .controller(ConfigScreen::getBooleanBuilder)
                            .build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.disconnect.label"))
                            .description(OptionDescription.createBuilder()
                                    .text(translatable("eventutils.config.disconnect.description"))
                                    .image(Identifier.of(BuildProperties.MOD_ID, "textures/config/confirm_disconnect.png"), 972, 295)
                                    .build())
                            .binding(EUConfig.Defaults.CONFIRM_DISCONNECT, () -> config.confirm_disconnect, newValue -> {
                                config.confirm_disconnect = newValue;
                                config.save();
                            })
                            .controller(ConfigScreen::getBooleanBuilder)
                            .build())
                    .option(Option.<Boolean>createBuilder()
                            .name(translatable("eventutils.config.bee_icons.label"))
                            .description(OptionDescription.createBuilder()
                                    .text(translatable("eventutils.config.bee_icons.description"))
                                    .image(Identifier.of(BuildProperties.MOD_ID, "textures/config/bee_icons.png"), 391, 61)
                                    .build())
                            .binding(EUConfig.Defaults.BEE_ICONS, () -> config.bee_icons, newValue -> {
                                config.bee_icons = newValue;
                                config.save();
                            })
                            .controller(ConfigScreen::getBooleanBuilder)
                            .build())
                    .option(ButtonOption.createBuilder()
                            .name(translatable("eventutils.config.groups.manage.label"))
                            .description(OptionDescription.of(translatable("eventutils.config.groups.manage.description")))
                            .text(translatable("eventutils.config.groups.manage.button"))
                            .action((yaclScreen, option) -> MinecraftClient.getInstance().setScreen(new GroupManagerScreen(EventUtils.MOD, yaclScreen)))
                            .build())
                    // Advanced
                    .group(OptionGroup.createBuilder()
                            .name(translatable("eventutils.config.advanced.category"))
                            .collapsed(true)
                            .option(Option.<Boolean>createBuilder()
                                    .name(translatable("eventutils.config.advanced.developer_mode.label"))
                                    .description(OptionDescription.of(translatable("eventutils.config.advanced.developer_mode.description")))
                                    .binding(EUConfig.Defaults.DEVELOPER_MODE, () -> config.developer_mode, newValue -> {
                                        config.developer_mode = newValue;
                                        config.save();
                                        EventUtils.MOD.updateLogLevel();
                                        EventUtils.MOD.setupSdk("Developer Mode enabled/disabled");
                                        EventUtils.MOD.cacheManager.clearAll();
                                        EventUtils.MOD.authManager.authenticate().queue();
                                    })
                                    .controller(ConfigScreen::getBooleanBuilder)
                                    .build())
                            .option(Option.<StandardLevel>createBuilder()
                                    .name(translatable("eventutils.config.advanced.log_level.label"))
                                    .description(OptionDescription.of(translatable("eventutils.config.advanced.log_level.description")))
                                    .binding(EUConfig.Defaults.LOG_LEVEL, () -> config.log_level, newValue -> {
                                        config.log_level = newValue;
                                        config.save();
                                        EventUtils.MOD.updateLogLevel();
                                    })
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(StandardLevel.class))
                                    .build())
                            .build())
                    .build());

        // Events
        final ConfigCategory.Builder alertsCategory = ConfigCategory.createBuilder()
                .name(translatable("eventutils.config.event_settings.category"))
                .option(Option.<String>createBuilder()
                        .name(translatable("eventutils.config.famous.label"))
                        .description(OptionDescription.of(translatable("eventutils.config.famous.description")))
                        .binding(EUConfig.Defaults.DEFAULT_FAMOUS_IP, () -> config.default_famous_ip, newValue -> {
                            config.default_famous_ip = newValue;
                            config.save();
                        })
                        .controller(StringControllerBuilder::create)
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(translatable("eventutils.config.server_list_minutes.label"))
                        .description(OptionDescription.of(translatable("eventutils.config.server_list_minutes.description")))
                        .binding(EUConfig.Defaults.EVENT_SERVER_DISPLAY_MINUTES, () -> config.event_server_display_minutes, config::setEventServerDisplayMinutes)
                        .controller(option -> IntegerFieldControllerBuilder.create(option)
                                .range(EUConfig.MIN_EVENT_SERVER_DISPLAY_MINUTES, EUConfig.MAX_EVENT_SERVER_DISPLAY_MINUTES))
                        .build());
        for (final EventType type : EventType.values()) alertsCategory.group(type.getOptionGroup(config));
        builder.category(alertsCategory.build());

        // Return
        return builder.build().generateScreen(parent);
    }

    @NotNull
    public static BooleanControllerBuilder getBooleanBuilder(@NotNull Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).coloured(true).onOffFormatter();
    }
}
