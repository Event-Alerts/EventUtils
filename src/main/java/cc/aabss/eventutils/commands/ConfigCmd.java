package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.config.ConfigScreen;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;

import org.jetbrains.annotations.NotNull;


public class ConfigCmd {
    public static void config(@NotNull CommandContext<FabricClientCommandSource> context) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> client.setScreen(ConfigScreen.getConfigScreen(client.currentScreen)));
    }
}
