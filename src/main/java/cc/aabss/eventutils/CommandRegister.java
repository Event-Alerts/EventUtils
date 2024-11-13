package cc.aabss.eventutils;

import cc.aabss.eventutils.config.ConfigScreen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.text.Text.translatable;

public class CommandRegister {
    public static void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // eventutils
        final LiteralCommandNode<FabricClientCommandSource> main = ClientCommandManager
                .literal("eventutils")
                .executes(context -> {
                    help(context);
                    return 0;
                }).build();

        // eventutils config
        final LiteralCommandNode<FabricClientCommandSource> config = ClientCommandManager
                .literal("config")
                .executes(context -> {
                    config(context);
                    return 0;
                }).build();

        // eventutils teleport
        final LiteralCommandNode<FabricClientCommandSource> teleport = ClientCommandManager
                .literal("teleport")
                .executes(context -> {
                    teleport(context, null);
                    return 0;
                }).build();
        for (final EventType type : EventType.values()) teleport.addChild(ClientCommandManager
                .literal(type.name().toLowerCase())
                .executes((context -> {
                    teleport(context, type);
                    return 0;
                })).build());

        // eventutils pickup priority
        final LiteralCommandNode<FabricClientCommandSource> priority = ClientCommandManager
                .literal("priority")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (AbstractClientPlayerEntity player : context.getSource().getWorld().getPlayers()) {
                                builder.suggest(player.getName().getString());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            priority(context, StringArgumentType.getString(context, "player"));
                            return 0;
                        }))
                .executes(context -> {
                    priority(context, context.getSource().getPlayer().getName().getString());
                    return 0;
                }).build();

        final LiteralCommandNode<FabricClientCommandSource> priorityTop = ClientCommandManager
                .literal("prioritytop")
                .then(ClientCommandManager.argument("page", IntegerArgumentType.integer())
                        .executes((context) -> {
                            priority(context, IntegerArgumentType.getInteger(context, "page"));
                            return 0;
                        })
                )
                .executes(context -> {
                    priority(context, 1);
                    return 0;
                }).build();

        // Build command tree
        dispatcher.getRoot().addChild(main);
        main.addChild(config);
        main.addChild(teleport);
        main.addChild(priority);
        main.addChild(priorityTop);
    }

    private static void help(@NotNull CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendError(Text.literal("/eventutils config - " + EventUtils.translate("eventutils.command.config") + "\n/eventutils teleport <type> - " + EventUtils.translate("eventutils.command.teleport")));
    }

    private static void config(@NotNull CommandContext<FabricClientCommandSource> context) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> client.setScreen(ConfigScreen.getConfigScreen(client.currentScreen)));
    }

    private static void priority(@NotNull CommandContext<FabricClientCommandSource> context, String name) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            assert client.world != null;
            List<String> namesSorted = client.world.getPlayers().stream()
                    .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                    .map(player -> player.getName().getString())
                    .toList();
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (name.equalsIgnoreCase(player.getName().getString())) {
                    context.getSource().sendFeedback(Text.literal(name+" has pickup priority #" + namesSorted.indexOf(name) + " (based on people around you)"));
                    return;
                }
            }
            context.getSource().sendFeedback(Text.literal("No player found.").formatted(Formatting.RED));
        });
    }

    private static void priority(@NotNull CommandContext<FabricClientCommandSource> context, int page) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            assert client.world != null;
            assert client.player != null;
            List<String> namesSorted = client.world.getPlayers().stream()
                    .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                    .map(player -> player.getName().getString())
                    .toList();
            int playersPerPage = 17; // maybe can change later
            int totalPlayers = namesSorted.size();
            int totalPages = (int) Math.ceil((double) totalPlayers / playersPerPage);
            int pageIndex = Math.max(0, page - 1);
            int start = pageIndex * playersPerPage;
            int end = Math.min(start + playersPerPage, totalPlayers);
            if (page > totalPages || page < 1) {
                context.getSource().sendFeedback(Text.literal("No page exists. (" + totalPages+")").formatted(Formatting.RED));
                return;
            }
            MutableText text = Text.literal("\nPage " + page + " of " + totalPages + ":\n");
            for (int i = start; i < end; i++) {
                String name = namesSorted.get(i);
                if (name.equalsIgnoreCase(client.player.getName().getString())) {
                    text.append(Text.literal(i + 1+". "+namesSorted.get(i)+"\n").formatted(Formatting.YELLOW));
                } else {
                    text.append(i + 1+". "+namesSorted.get(i)+"\n");
                }
            }
            context.getSource().sendFeedback(text);
        });
    }

    private static void teleport(@NotNull CommandContext<FabricClientCommandSource> context, @Nullable EventType type) {
        final FabricClientCommandSource source = context.getSource();
        if (type == null) {
            source.sendError(translatable("eventutils.command.no_event_specified"));
            return;
        }

        // Get lastIp
        final String lastIp = EventUtils.MOD.lastIps.get(type);
        if (lastIp == null) {
            source.sendError(translatable("eventutils.command.no_event_found").append(Text.literal(type.displayNameString.toLowerCase() + "!")));
            return;
        }

        ConnectUtility.connect(lastIp);
    }
}
