package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.utility.ConnectUtility;
import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.text.Text.translatable;


public class TeleportCmd {
    public static void teleport(@NotNull CommandContext<FabricClientCommandSource> context, @Nullable EventType type) {
        final FabricClientCommandSource source = context.getSource();
        if (type == null) {
            source.sendError(translatable("eventutils.command.no_event_specified"));
            return;
        }

        // Get lastIp
        final String lastIp = EventUtils.MOD.lastIps.get(type);
        if (lastIp == null) {
            source.sendError(translatable("eventutils.command.no_event_found").append(Text.literal(type.displayNameString.toLowerCase() + "!")));
            return;
        }

        // Connect
        ConnectUtility.connect(lastIp);
    }
}
