package cc.aabss.eventutils.commands.priority;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.commands.EUCommand;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class PriorityCmd extends EUCommand {
    public PriorityCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @Override @NotNull
    public LiteralCommandNode<FabricClientCommandSource> build() {
        return ClientCommandManager
                .literal("priority")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            final ClientPacketListener packetListener = context.getSource().getClient().getConnection();
                            if (packetListener != null) for (final PlayerInfo player : packetListener.getListedOnlinePlayers()) {
                                final VersionedGameProfile profile = new VersionedGameProfile(player.getProfile());
                                if (!EventUtils.isNpc(profile.getId())) builder.suggest(profile.getName());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            execute(context, StringArgumentType.getString(context, "player"));
                            return 0;
                        }))
                .executes(context -> {
                    execute(context, context.getSource().getPlayer().getName().getString());
                    return 0;
                })
                .build();
    }

    private static void execute(@NotNull CommandContext<FabricClientCommandSource> context, String name) {
        final Minecraft client = context.getSource().getClient();
        client.execute(() -> {
            // Get names
            final List<String> namesSorted = PriorityCommon.getNamesSorted(client);
            if (namesSorted == null || client.player == null) {
                context.getSource().sendFeedback(Component.translatable("eventutils.command.priority.noplayer", EventUtils.ERROR_MESSAGE_PREFIX));
                return;
            }

            final String nameLower = name.toLowerCase();
            for (final String playerName : namesSorted) if (nameLower.equals(playerName.toLowerCase())) {
                if (playerName.equalsIgnoreCase(client.player.getName().getString())) {
                    context.getSource().sendFeedback(Component.translatable("eventutils.command.priority.self", EventUtils.MESSAGE_PREFIX, "§6#" + (namesSorted.indexOf(playerName) + 1)));
                } else {
                    context.getSource().sendFeedback(Component.translatable("eventutils.command.priority.player", EventUtils.MESSAGE_PREFIX, Component.literal(playerName).withStyle(ChatFormatting.YELLOW), "§6#" + (namesSorted.indexOf(playerName) + 1)));
                }

                return;
            }
            context.getSource().sendFeedback(Component.translatable("eventutils.command.priority.noplayer", EventUtils.ERROR_MESSAGE_PREFIX));
        });
    }
}
