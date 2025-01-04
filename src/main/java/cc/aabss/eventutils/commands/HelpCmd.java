package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;


public class HelpCmd {
    public static void help(@NotNull CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendError(Text.literal("/eventutils config - " + EventUtils.translate("eventutils.command.config") + "\n/eventutils teleport <type> - " + EventUtils.translate("eventutils.command.teleport")));
    }
}
