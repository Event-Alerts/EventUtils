package cc.aabss.eventutils.commands;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class NameDetectCmd {
    public static void detectName(@NotNull CommandContext<FabricClientCommandSource> context, String searchTerm) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) return;

        // Get the list of online players and count those whose names contain the search term
        long count = client.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(profile -> profile.getName().toLowerCase())
                .filter(name -> name.contains(searchTerm.toLowerCase()))
                .count();

        // Send the result message to the player using components
        context.getSource().getPlayer().sendMessage(
                Text.literal("Found ")
                        .append(Text.literal(String.valueOf(count)).styled(style -> style.withBold(true).withColor(0xFF5555)))
                        .append(" player(s) with '")
                        .append(Text.literal(searchTerm).styled(style -> style.withItalic(true)))
                        .append("' in their name.")
        );
    }
}
