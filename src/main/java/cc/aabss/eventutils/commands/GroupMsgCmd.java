package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.network.chat.Component.translatable;


public class GroupMsgCmd extends EUCommand {
    public GroupMsgCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @Override @NotNull
    public LiteralCommandNode<FabricClientCommandSource> build() {
        return ClientCommandManager
                .literal("groupmsg")
                .then(ClientCommandManager.argument("group", StringArgumentType.word())
                        .suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(() -> mod.config.getGroupNames().iterator(), builder))
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> execute(
                                        context,
                                        StringArgumentType.getString(context, "group"),
                                        StringArgumentType.getString(context, "message")))))
                .build();
    }

    private int execute(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull String groupName, @NotNull String message) {
        if (message.isEmpty()) return 0;

        // Get group
        final Group group = mod.config.getGroupByName(groupName);
        if (group == null) {
            context.getSource().sendError(translatable("eventutils.command.groupmsg.no_group", EventUtils.ERROR_MESSAGE_PREFIX, groupName));
            return 0;
        }

        // Check if any recipients
        if (group.getPlayers().isEmpty()) {
            context.getSource().sendError(translatable("eventutils.command.groupmsg.no_group", EventUtils.ERROR_MESSAGE_PREFIX, group.getName()));
            return 0;
        }

        // Message recipients
        final ClientPacketListener connection = context.getSource().getPlayer().connection;
        for (final String recipient : group.getPlayers()) {
            connection.sendCommand("msg " + recipient + " [" + group.getName() + "] " + message);
        }
        return 1;
    }
}
