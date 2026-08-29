package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.screen.config.ConfigScreen;
import cc.aabss.eventutils.versioning.VersionedClient;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
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
        final Minecraft client = context.getSource().getClient();
        client.execute(() -> {
            final VersionedClient vClient = new VersionedClient(client);
            vClient.setScreen(ConfigScreen.getConfigScreen(vClient.screen()));
        });
    }
}
