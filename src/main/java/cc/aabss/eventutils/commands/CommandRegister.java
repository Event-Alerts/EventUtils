package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventType;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.network.AbstractClientPlayerEntity;

import org.jetbrains.annotations.NotNull;


public class CommandRegister {
    public static void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // eventutils
        final LiteralCommandNode<FabricClientCommandSource> main = ClientCommandManager
                .literal("eventutils")
                .executes(context -> {
                    HelpCmd.help(context);
                    return 0;
                }).build();

        // eventutils config
        final LiteralCommandNode<FabricClientCommandSource> config = ClientCommandManager
                .literal("config")
                .executes(context -> {
                    ConfigCmd.config(context);
                    return 0;
                }).build();

        // eventutils teleport
        final LiteralCommandNode<FabricClientCommandSource> teleport = ClientCommandManager
                .literal("teleport")
                .executes(context -> {
                    TeleportCmd.teleport(context, null);
                    return 0;
                }).build();
        for (final EventType type : EventType.values()) teleport.addChild(ClientCommandManager
                .literal(type.name().toLowerCase())
                .executes((context -> {
                    TeleportCmd.teleport(context, type);
                    return 0;
                })).build());

        // eventutils pickup priority
        final LiteralCommandNode<FabricClientCommandSource> priority = ClientCommandManager
                .literal("priority")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (final AbstractClientPlayerEntity player : context.getSource().getWorld().getPlayers()) builder.suggest(player.getName().getString());
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            PriorityCmd.priority(context, StringArgumentType.getString(context, "player"));
                            return 0;
                        }))
                .executes(context -> {
                    PriorityCmd.priority(context, context.getSource().getPlayer().getName().getString());
                    return 0;
                }).build();

        final LiteralCommandNode<FabricClientCommandSource> priorityTop = ClientCommandManager
                .literal("prioritytop")
                .then(ClientCommandManager.argument("page", IntegerArgumentType.integer())
                        .executes((context) -> {
                            PriorityCmd.priority(context, IntegerArgumentType.getInteger(context, "page"));
                            return 0;
                        })
                )
                .executes(context -> {
                    PriorityCmd.priority(context, 1);
                    return 0;
                }).build();

        final LiteralCommandNode<FabricClientCommandSource> countName = ClientCommandManager
                .literal("countname")
                .then(ClientCommandManager.literal("count")
                        .then(ClientCommandManager.argument("filter", StringArgumentType.string())
                                .executes((context) -> {
                                    CountNameCmd.count(context, StringArgumentType.getString(context, "filter"));
                                    return 0;
                                })))
                .then(ClientCommandManager.literal("list")
                        .then(ClientCommandManager.argument("filter", StringArgumentType.string())
                                .executes((context) -> {
                                    CountNameCmd.list(context, StringArgumentType.getString(context, "filter"));
                                    return 0;
                                })))
                .executes(context -> {
                    CountNameCmd.list(context, "");
                    return 0;
                }).build();

        // Build command tree
        dispatcher.getRoot().addChild(main);
        main.addChild(config);
        main.addChild(teleport);
        main.addChild(priority);
        main.addChild(priorityTop);
        main.addChild(countName);
    }
}
