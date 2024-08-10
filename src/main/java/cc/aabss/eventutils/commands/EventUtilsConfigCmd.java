package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;


public class EventUtilsConfigCmd extends EventCommand {
    public EventUtilsConfigCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @Override
    protected void run(@NotNull CommandContext<FabricClientCommandSource> context) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            client.setScreen(mod.configScreen.build().generateScreen(client.currentScreen));
            if (client.player != null) client.player.sendMessage(Text.literal("Opening screen..").formatted(Formatting.GREEN));
        });
    }
}