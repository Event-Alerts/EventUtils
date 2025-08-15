package cc.aabss.eventutils.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DetectNameCmd {
    public static void detectName(@NotNull CommandContext<FabricClientCommandSource> context, String searchTerm) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            if (client.player == null || client.player.networkHandler == null) {
                context.getSource().sendFeedback(Text.literal("No players found!").formatted(Formatting.RED));
                return;
            }

            final List<PlayerListEntry> players = client.player.networkHandler.getPlayerList();
            final long count = players.stream()
                    .map(entry -> entry.getProfile().getName().toLowerCase())
                    .filter(name -> name.contains(searchTerm.toLowerCase()))
                    .count();

            MutableText message = Text.literal("Found ")
                    .append(Text.literal(String.valueOf(count)).formatted(Formatting.RED, Formatting.BOLD, Formatting.ITALIC))
                    .append(Text.literal(" player(s) with '").formatted(Formatting.GRAY))
                    .append(Text.literal(searchTerm).formatted(Formatting.WHITE))
                    .append(Text.literal("' in their name.").formatted(Formatting.GRAY));

            context.getSource().sendFeedback(message);
        });
    }
}
