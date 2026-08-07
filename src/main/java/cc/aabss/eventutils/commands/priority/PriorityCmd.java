package cc.aabss.eventutils.commands.priority;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.commands.EUCommand;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
                            final ClientPlayNetworkHandler networkHandler = context.getSource().getClient().getNetworkHandler();
                            if (networkHandler != null) for (final PlayerListEntry player : networkHandler.getPlayerList()) {
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
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            // Get names
            final List<String> namesSorted = PriorityCommon.getNamesSorted(client);
            if (namesSorted == null || client.player == null) {
                context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.noplayer", EventUtils.ERROR_MESSAGE_PREFIX));
                return;
            }

            final String nameLower = name.toLowerCase();
            for (final String playerName : namesSorted) if (nameLower.equals(playerName.toLowerCase())) {
                if (playerName.equalsIgnoreCase(client.player.getName().getString())) {
                    context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.self", EventUtils.MESSAGE_PREFIX, "§6#" + (namesSorted.indexOf(playerName) + 1)));
                } else {
                    context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.player", EventUtils.MESSAGE_PREFIX, Text.literal(playerName).formatted(Formatting.YELLOW), "§6#" + (namesSorted.indexOf(playerName) + 1)));
                }

                return;
            }
            context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.noplayer", EventUtils.ERROR_MESSAGE_PREFIX));
        });
    }
}
