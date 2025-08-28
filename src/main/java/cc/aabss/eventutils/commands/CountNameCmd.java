package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CountNameCmd {
    public static void count(@NotNull CommandContext<FabricClientCommandSource> context, String filter) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
                context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.noplayers", Text.literal(filter).formatted(Formatting.DARK_RED)));
                return;
            }

            final List<String> namesFiltered = client.getNetworkHandler().getPlayerList().stream()
                    .map(entry -> entry.getProfile().getName())
                    .filter(name -> name.toLowerCase().contains(filter.toLowerCase()))
                    .filter(name -> !EventUtils.isNPC(name, true))
                    .toList();

            if (namesFiltered.isEmpty()) {
                context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.noplayers", Text.literal(filter).formatted(Formatting.DARK_RED)));
                return;
            }

            context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.count", "§6" + namesFiltered.size(), namesFiltered.size() != 1 ? Text.translatable("eventutils.word.plural").formatted(Formatting.YELLOW) : "", Text.literal(filter).formatted(Formatting.GOLD)));
        });
    }

    public static void list(@NotNull CommandContext<FabricClientCommandSource> context, String filter) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
                context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.noplayers", Text.literal(filter).formatted(Formatting.DARK_RED)));
                return;
            }

            final List<String> namesFiltered = client.getNetworkHandler().getPlayerList().stream()
                    .map(entry -> entry.getProfile().getName())
                    .filter(name -> name.toLowerCase().contains(filter.toLowerCase()))
                    .filter(name -> !EventUtils.isNPC(name, true))
                    .toList();

            if (namesFiltered.isEmpty()) {
                context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.noplayers", Text.literal(filter).formatted(Formatting.DARK_RED)));
                return;
            }

            MutableText playerList = Text.literal("");
            for (int i = 0; i < namesFiltered.size(); i++) {
                String name = namesFiltered.get(i);
                playerList.append(
                        Text.literal(name).formatted(Formatting.GOLD)
                );

                if (i < namesFiltered.size() - 1) {
                    playerList.append(
                            Text.literal(", ").formatted(Formatting.YELLOW)
                    );
                }
            }

            context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.list", "§6" + namesFiltered.size(), namesFiltered.size() != 1 ? Text.translatable("eventutils.word.plural").formatted(Formatting.YELLOW) : "", Text.literal(filter).formatted(Formatting.GOLD), playerList));
        });
    }
}