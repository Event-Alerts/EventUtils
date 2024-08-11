package cc.aabss.eventutils;

import cc.aabss.eventutils.config.ConfigScreen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        // Build command tree
        dispatcher.getRoot().addChild(main);
        main.addChild(config);
        main.addChild(teleport);
    }

    private static void help(@NotNull CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendError(Text.literal("/eventutils config - " + EventUtils.translate("eventutils.command.config") + "\n/eventutils teleport <type> - " + EventUtils.translate("eventutils.command.teleport")));
    }

    private static void config(@NotNull CommandContext<FabricClientCommandSource> context) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> client.setScreen(ConfigScreen.getConfigScreen(client.currentScreen)));
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
