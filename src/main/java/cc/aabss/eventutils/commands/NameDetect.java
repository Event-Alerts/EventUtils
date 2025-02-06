package cc.aabss.eventutils.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;

public class NameDetectCommand {
    public static void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // detectname command
        final LiteralCommandNode<FabricClientCommandSource> detectName = ClientCommandManager
                .literal("detectname")
                .then(ClientCommandManager.argument("word", StringArgumentType.string())
                        .executes(context -> {
                            String searchTerm = StringArgumentType.getString(context, "word").toLowerCase();
                            detectName(context.getSource(), searchTerm);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        
        dispatcher.getRoot().addChild(detectName);
    }

    private static void detectName(FabricClientCommandSource source, String searchTerm) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) return;

        // Get the list of online players and count those whose names contain the search term
        long count = client.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(profile -> profile.getName().toLowerCase())
                .filter(name -> name.contains(searchTerm))
                .count();

        // Send the result message to the player
        source.getPlayer().sendMessage(Text.of("§7Found §4§o§l" + count + "§r§7 player(s) with '" + searchTerm + "' in their name."), false);
    }
}
