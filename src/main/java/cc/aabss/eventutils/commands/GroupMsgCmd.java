package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static net.minecraft.text.Text.translatable;


public class GroupMsgCmd {
    public static int groupMsg(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull String payload) {
        final String input = payload.trim();
        if (input.isEmpty()) return 0;
        final Group group = findLongestPrefixGroup(input);
        if (group == null) {
            context.getSource().sendError(translatable("eventutils.command.groupmsg.no_group", EventUtils.ERROR_MESSAGE_PREFIX, input));
            return 0;
        }
        final String message = input.substring(group.getName().length()).trim();
        if (message.isEmpty()) return 0;
        return sendToGroup(context, group, message);
    }

    private static int sendToGroup(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull Group group, @NotNull String message) {
        final var sender = context.getSource().getPlayer();
        if (sender.networkHandler == null) return 0;

        final List<String> recipients = group.getPlayers().stream()
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            context.getSource().sendError(translatable("eventutils.command.groupmsg.no_group", EventUtils.ERROR_MESSAGE_PREFIX, group.getName()));
            return 0;
        }
        for (final String recipient : recipients) {
            sender.networkHandler.sendChatCommand("msg " + recipient + " [Group: " + group.getName() + "] " + message);
        }
        return 1;
    }

    @Nullable
    private static Group findLongestPrefixGroup(@NotNull String input) {
        Group best = null;
        int bestLen = -1;
        final String lowered = input.toLowerCase(Locale.ROOT);
        for (final Group group : EventUtils.MOD.config.groups.values()) {
            final String groupName = group.getName().toLowerCase(Locale.ROOT);
            if (groupName.isEmpty()) continue;
            if (!lowered.startsWith(groupName)) continue;
            if (lowered.length() > groupName.length() && lowered.charAt(groupName.length()) != ' ') continue;
            if (groupName.length() > bestLen) {
                best = group;
                bestLen = groupName.length();
            }
        }
        return best;
    }
}
