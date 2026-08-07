package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.sdk.EventWrapper;
import cc.aabss.eventutils.utility.ConnectUtility;
import cc.aabss.eventutils.config.EventType;
import cc.aabss.eventutils.EventUtils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.text.Text.translatable;


public class TeleportCmd extends EUCommand {
    public TeleportCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @Override @NotNull
    public LiteralCommandNode<FabricClientCommandSource> build() {
        final LiteralCommandNode<FabricClientCommandSource> teleport = ClientCommandManager
            .literal("teleport")
            .executes(context -> {
                execute(context, null);
                return 0;
            })
            .build();
        for (final EventType type : EventType.values()) teleport.addChild(ClientCommandManager
                .literal(type.name().toLowerCase())
                .executes((context -> {
                    execute(context, type);
                    return 0;
                }))
                .build());
        return teleport;
    }

    private void execute(@NotNull CommandContext<FabricClientCommandSource> context, @Nullable EventType type) {
        final FabricClientCommandSource source = context.getSource();
        if (type == null) {
            source.sendError(translatable("eventutils.command.no_event_specified"));
            return;
        }

        // Get last event of type
        final EventWrapper lastEvent = mod.lastEvents.get(type);
        if (lastEvent == null || lastEvent.ip == null) {
            source.sendError(translatable("eventutils.command.no_event_found", type.name().toLowerCase()));
            return;
        }

        // Connect
        EventUtils.LOGGER.debug("Connecting to {} for event {}", lastEvent.ip, lastEvent);
        ConnectUtility.connect(lastEvent.ip);
    }
}
