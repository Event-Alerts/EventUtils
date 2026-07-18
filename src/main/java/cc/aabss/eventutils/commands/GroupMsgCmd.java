package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.text.Text.translatable;


public class GroupMsgCmd {
    public static int groupMsg(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull String groupName, @NotNull String message) {
        if (message.isEmpty()) return 0;

        final ClientPlayerEntity sender = context.getSource().getPlayer();
        if (sender.networkHandler == null) return 0;

        // Get group
        final Group group = EventUtils.MOD.config.getGroupByName(groupName);
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
        for (final String recipient : group.getPlayers()) {
            sender.networkHandler.sendChatCommand("msg " + recipient + " [" + group.getName() + "] " + message);
        }
        return 1;
    }
}
