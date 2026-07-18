package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
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
                            final ClientPlayNetworkHandler networkHandler = context.getSource().getClient().getNetworkHandler();
                            if (networkHandler != null) for (final PlayerListEntry player : networkHandler.getPlayerList()) {
                                final VersionedGameProfile profile = new VersionedGameProfile(player.getProfile());
                                if (!EventUtils.isNpc(profile.getId())) builder.suggest(profile.getName());
                            }
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

        final LiteralCommandNode<FabricClientCommandSource> groupMsg = ClientCommandManager
                .literal("groupmsg")
                .then(ClientCommandManager.argument("group", StringArgumentType.word())
                        .suggests((context, builder) ->
                                CommandSource.suggestMatching(() -> EventUtils.MOD.config.getGroupNames().iterator(), builder))
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> GroupMsgCmd.groupMsg(
                                        context,
                                        StringArgumentType.getString(context, "group"),
                                        StringArgumentType.getString(context, "message")))))
                .build();

        // Build command tree
        dispatcher.getRoot().addChild(groupMsg);
        dispatcher.getRoot().addChild(main);
        main.addChild(config);
        main.addChild(teleport);
        main.addChild(priority);
        main.addChild(priorityTop);
        main.addChild(countName);
        main.addChild(groupMsg);
    }
}
