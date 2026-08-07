package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.screen.config.ConfigScreen;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;


public class ConfigCmd extends EUCommand {
    public ConfigCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @Override @NotNull
    public LiteralCommandNode<FabricClientCommandSource> build() {
        return ClientCommandManager
                .literal("config")
                .executes(context -> {
                    execute(context);
                    return 0;
                })
                .build();
    }

    private static void execute(@NotNull CommandContext<FabricClientCommandSource> context) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> client.setScreen(ConfigScreen.getConfigScreen(client.currentScreen)));
    }
}
