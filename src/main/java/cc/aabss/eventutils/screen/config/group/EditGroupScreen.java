package cc.aabss.eventutils.screen.config.group;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.network.chat.Component.translatable;


public class EditGroupScreen {
    @NotNull private static final List<String> ENTITY_TYPES = BuiltInRegistries.ENTITY_TYPE.stream()
            .map(entityType -> EntityType.getKey(entityType).toString())
            .toList();

    @NotNull
    public static Screen getScreen(@NotNull EventUtils mod, @NotNull Screen parent, @NotNull Group group) {
        return YetAnotherConfigLib.createBuilder()
                .title(translatable("eventutils.config.groups.edit_title"))
                .category(ConfigCategory.createBuilder().name(translatable("eventutils.config.groups.edit_title"))
                        .option(Option.<String>createBuilder()
                                .name(translatable("eventutils.config.groups.name.label"))
                                .binding(group.getUuid().toString(), group::getName, newValue -> {
                                    // Check if group with NEW name already exists
                                    if (!newValue.equals(group.getName()) && mod.config.groups.getByName(newValue) != null) return;

                                    group.setName(newValue);
                                    mod.config.save();
                                })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(translatable("eventutils.config.groups.radius.label"))
                                .binding(
                                        Objects.requireNonNullElse(Group.Defaults.RADIUS, Group.MAX_RADIUS + 1),
                                        () -> Objects.requireNonNullElse(group.getRadius(), Group.MAX_RADIUS + 1),
                                        newValue -> {
                                            if (newValue > Group.MAX_RADIUS) newValue = null;

                                            group.setRadius(newValue);
                                            mod.config.save();
                                        })
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(1, Group.MAX_RADIUS + 1)
                                        .step(1)
                                        .formatValue(value -> {
                                            final Component valueText = value > Group.MAX_RADIUS
                                                    ? translatable("eventutils.config.groups.radius.infinite")
                                                    : Component.literal(value.toString());
                                            return translatable("eventutils.config.groups.radius.value", valueText);
                                        }))
                                .build())
                        .option(Option.<Group.Mode>createBuilder()
                                .name(translatable("eventutils.config.groups.player_mode.label"))
                                .binding(Group.Defaults.PLAYER_MODE, group::getPlayerMode, newValue -> {
                                    group.setPlayerMode(newValue);
                                    mod.config.save();
                                })
                                .controller(EditGroupScreen::getModeController)
                                .build())
                        .option(Option.<Group.Mode>createBuilder()
                                .name(translatable("eventutils.config.groups.entity_mode.label"))
                                .binding(Group.Defaults.ENTITY_MODE, group::getEntityMode, newValue -> {
                                    group.setEntityMode(newValue);
                                    mod.config.save();
                                })
                                .controller(EditGroupScreen::getModeController)
                                .build())
                        .option(Option.<Group.Mode>createBuilder()
                                .name(translatable("eventutils.config.groups.npc_mode.label"))
                                .binding(Group.Defaults.NPC_MODE, group::getNpcMode, newValue -> {
                                    group.setNpcMode(newValue);
                                    mod.config.save();
                                })
                                .controller(EditGroupScreen::getModeController)
                                .build())
                        .group(ListOption.<String>createBuilder()
                                .name(translatable("eventutils.config.groups.players.label"))
                                .binding(new ArrayList<>(Group.Defaults.players()), () -> new ArrayList<>(group.getPlayers()), newValue -> {
                                    group.setPlayers(newValue);
                                    mod.config.save();
                                })
                                .initial(new VersionedGameProfile(Minecraft.getInstance().getGameProfile()).getName())
                                .controller(StringControllerBuilder::create)
                                .build())
                        .group(ListOption.<String>createBuilder()
                                .name(translatable("eventutils.config.groups.entities.label"))
                                .binding(new ArrayList<>(Group.Defaults.entityIds()), () -> new ArrayList<>(group.getEntityIds()), newValue -> {
                                    group.setEntitiesByIds(newValue);
                                    mod.config.save();
                                })
                                .initial("minecraft:bee")
                                .controller(option -> DropdownStringControllerBuilder.create(option).values(ENTITY_TYPES))
                                .build())
                        .build())
                .build().generateScreen(parent);
    }

    @NotNull
    private static EnumControllerBuilder<Group.Mode> getModeController(@NotNull Option<Group.Mode> option) {
        return EnumControllerBuilder.create(option)
                .enumClass(Group.Mode.class)
                .formatValue(value -> translatable("eventutils.config.groups.mode." + value.name()).withStyle(value.formatting));
    }
}
