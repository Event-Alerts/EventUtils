package cc.aabss.eventutils;

import cc.aabss.eventutils.config.ConfigScreen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

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
                .executes(context -> {
                    priority(context, null);
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
            HashMap<String, Integer> map = new HashMap<>();
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                map.put(player.getName().getString(), player.getId());
            }
            List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
            entryList.sort(Map.Entry.comparingByValue());
            Map<String, Integer> sortedMap = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : entryList) {
                sortedMap.put(entry.getKey(), entry.getValue());
            }
            List<String> namesSorted = new ArrayList<>();
            sortedMap.forEach((key, value) -> namesSorted.add(key));
            StringBuilder builder = new StringBuilder();
            int i = 1;
            for (String player : namesSorted) {
                builder.append(i).append(". ").append(player).append("\n");
                if (Objects.equals(name, player) && name != null) {
                    context.getSource().sendFeedback(Text.literal(name+" has pickup priority #" + i + " (based on people around you)"));
                    return;
                }
                i++;
            }
            context.getSource().sendFeedback(Text.literal(builder.toString()));
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
