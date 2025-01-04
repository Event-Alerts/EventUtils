package cc.aabss.eventutils.commands;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;


public class PriorityCmd {
    private static final int PLAYERS_PER_PAGE = 17; // maybe can change later

    public static void priority(@NotNull CommandContext<FabricClientCommandSource> context, String name) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            // Get names
            if (client.world == null) {
                context.getSource().sendFeedback(Text.literal("Player not found!!").formatted(Formatting.RED));
                return;
            }
            final List<String> namesSorted = client.world.getPlayers().stream()
                    .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                    .map(player -> player.getName().getString())
                    .toList();

            final String nameLower = name.toLowerCase();
            for (final String playerName : namesSorted) if (nameLower.equals(playerName.toLowerCase())) {
                context.getSource().sendFeedback(Text.literal(name + " has pickup priority #" + (namesSorted.indexOf(name) + 1) + " (based on people around you)"));
                return;
            }
            context.getSource().sendFeedback(Text.literal("Player not found!!").formatted(Formatting.RED));
        });
    }

    public static void priority(@NotNull CommandContext<FabricClientCommandSource> context, int page) {
        final FabricClientCommandSource source = context.getSource();
        final MinecraftClient client = source.getClient();
        client.send(() -> {
            // Get names
            if (client.world == null || client.player == null) {
                source.sendFeedback(Text.literal("No players found!").formatted(Formatting.RED));
                return;
            }
            final List<String> namesSorted = client.world.getPlayers().stream()
                    .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                    .map(player -> player.getName().getString())
                    .toList();

            // Check page bounds
            final int totalPlayers = namesSorted.size();
            final int totalPages = (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE);
            if (page > totalPages || page < 1) {
                source.sendFeedback(Text.literal("No page exists (limit: " + totalPages + ")!").formatted(Formatting.RED));
                return;
            }

            // Send page
            final int pageIndex = Math.max(0, page - 1);
            final int start = pageIndex * PLAYERS_PER_PAGE;
            final int end = Math.min(start + PLAYERS_PER_PAGE, totalPlayers);
            final String clientName = client.player.getName().getString().toLowerCase();
            final MutableText text = Text.literal("\nPage " + page + " of " + totalPages + ":\n");
            for (int i = start; i < end; i++) {
                final String name = namesSorted.get(i);
                final MutableText item = Text.literal((i + 1) + ". " + name + "\n");
                if (name.toLowerCase().equals(clientName)) item.formatted(Formatting.YELLOW);
                text.append(item);
            }
            source.sendFeedback(text);
        });
    }
}
