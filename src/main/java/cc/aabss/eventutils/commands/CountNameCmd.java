package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class CountNameCmd {
    @Nullable
    private static List<String> getFilteredNames(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull MinecraftClient client, @NotNull String filter) {
        // No players available
        if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
            context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.noplayers", EventUtils.ERROR_MESSAGE_PREFIX, Text.literal(filter).formatted(Formatting.DARK_RED)));
            return null;
        }

        // Filter names based on config and "isNPC"
        final List<String> namesFiltered = client.getNetworkHandler().getPlayerList().stream()
                .map(entry -> new VersionedGameProfile(entry.getProfile()))
                .filter(profile -> !EventUtils.isNpc(profile.getId()))
                .map(VersionedGameProfile::getName)
                .filter(name -> name.toLowerCase().contains(filter.toLowerCase()))
                .toList();

        // No names after filtering
        if (namesFiltered.isEmpty()) {
            context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.noplayers", EventUtils.ERROR_MESSAGE_PREFIX, Text.literal(filter).formatted(Formatting.DARK_RED)));
            return null;
        }

        return namesFiltered;
    }

    public static void count(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull String filter) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            // Get names
            final List<String> namesFiltered = getFilteredNames(context, client, filter);
            if (namesFiltered == null) return;

            // Reply
            context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.count", EventUtils.MESSAGE_PREFIX, "§6" + namesFiltered.size(), namesFiltered.size() != 1 ? Text.translatable("eventutils.word.plural").formatted(Formatting.YELLOW) : "", Text.literal(filter).formatted(Formatting.GOLD)));
        });
    }

    public static void list(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull String filter) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            // Get names
            final List<String> namesFiltered = getFilteredNames(context, client, filter);
            if (namesFiltered == null) return;

            // Build player list
            final MutableText playerList = Text.literal("");
            for (int i = 0; i < namesFiltered.size(); i++) {
                playerList.append(Text.literal(namesFiltered.get(i)).formatted(Formatting.GOLD));
                // Add comma if not last
                if (i < namesFiltered.size() - 1) playerList.append(Text.literal(", ").formatted(Formatting.YELLOW));
            }

            // Reply
            context.getSource().sendFeedback(Text.translatable("eventutils.command.countname.list", EventUtils.MESSAGE_PREFIX, "§6" + namesFiltered.size(), namesFiltered.size() != 1 ? Text.translatable("eventutils.word.plural").formatted(Formatting.YELLOW) : "", Text.literal(filter).formatted(Formatting.GOLD), playerList));
        });
    }
}