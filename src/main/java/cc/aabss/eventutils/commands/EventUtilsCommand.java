package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.config.EventUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EventUtilsCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("eventutilsconfig")
                                .executes((context) -> run(context.getSource().getClient())));
    }

    public static int run(MinecraftClient client) {
        client.send(() -> {
            client.setScreen(EventUtil.screen(client.currentScreen));
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Opening screen..").formatted(Formatting.GREEN));
            }
        });
        return 1;
    }
}