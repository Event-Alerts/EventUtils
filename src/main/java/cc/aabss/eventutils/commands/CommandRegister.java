package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.commands.priority.PriorityCmd;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.commands.priority.PriorityTopCmd;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;


public class CommandRegister {
    public static void register(@NotNull EventUtils mod, @NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Commands
        final LiteralCommandNode<FabricClientCommandSource> config = new ConfigCmd(mod).build();
        final LiteralCommandNode<FabricClientCommandSource> teleport = new TeleportCmd(mod).build();
        final LiteralCommandNode<FabricClientCommandSource> priority = new PriorityCmd(mod).build();
        final LiteralCommandNode<FabricClientCommandSource> priorityTop = new PriorityTopCmd(mod).build();
        final LiteralCommandNode<FabricClientCommandSource> countName = new CountNameCmd(mod).build();
        final LiteralCommandNode<FabricClientCommandSource> groupMsg = new GroupMsgCmd(mod).build();

        // Root
        final LiteralCommandNode<FabricClientCommandSource> main = ClientCommandManager
                .literal("eventutils")
                .executes(context -> {
                    context.getSource().sendError(Component.translatable("eventutils.command.unknown"));
                    return 0;
                })
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
