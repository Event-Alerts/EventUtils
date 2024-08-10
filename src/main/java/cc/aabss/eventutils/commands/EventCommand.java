package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.ConnectUtility;
import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.ConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class EventCommand {
    @NotNull protected final CommandDispatcher<FabricClientCommandSource> dispatcher;
    protected final int returnValue = 1;

    public EventCommand(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        this.dispatcher = dispatcher;
        register();
    }

    private void register() {
        LiteralCommandNode<FabricClientCommandSource> main = ClientCommandManager
                .literal("eventutils")
                .executes(this::help)
                .build();
        LiteralCommandNode<FabricClientCommandSource> config = ClientCommandManager
                .literal("config")
                .executes(this::config)
                .build();
        LiteralCommandNode<FabricClientCommandSource> teleport = ClientCommandManager
                .literal("teleport")
                .executes(context -> teleport(context, null))
                .build();
        LiteralCommandNode<FabricClientCommandSource> help = ClientCommandManager
                .literal("help")
                .executes(this::help)
                .build();
        List<LiteralCommandNode<FabricClientCommandSource>> eventTypes = new ArrayList<>();
        for (EventType eventType : EventType.values()) {
            eventTypes.add(ClientCommandManager
                    .literal(eventType.name().toLowerCase())
                    .executes((context -> teleport(context, eventType)))
                    .build());
        }
        dispatcher.getRoot().addChild(main);
        main.addChild(config);
        main.addChild(teleport);
        main.addChild(help);
        for (LiteralCommandNode<FabricClientCommandSource> eventType : eventTypes) {
            teleport.addChild(eventType);
        }
    }

    private int config(CommandContext<FabricClientCommandSource> context) {
        context.getSource().getPlayer().sendMessage(Text.literal("Opening screen...").formatted(Formatting.GREEN));
        context.getSource().getClient().send(() ->
                context.getSource().getClient().setScreen(ConfigScreen.getConfigScreen(context.getSource().getClient().currentScreen))
        );
        return returnValue;
    }

    private int teleport(CommandContext<FabricClientCommandSource> context, EventType eventType) {
        if (eventType == null) {
            context.getSource().getPlayer().sendMessage(Text.literal("No event type specified."));
            return returnValue;
        }
        if (!EventUtils.MOD.lastIps.containsKey(eventType)) {
            context.getSource().getPlayer().sendMessage(Text.literal("No event found for "+eventType.displayName.toLowerCase()+".").formatted(Formatting.RED));
            return returnValue;
        }
        ConnectUtility.connect(EventUtils.MOD.lastIps.get(eventType));
        return returnValue;
    }

    private int help(CommandContext<FabricClientCommandSource> context) {
        context.getSource().getPlayer().sendMessage(
                Text.literal("""
                        /eventutils config - Opens config screen.
                        /eventutils teleport <eventType> - Teleports to the last posted event of that type.
                        /eventutils help - Shows this screen."""
                ).formatted(Formatting.RED));
        return returnValue;
    }
}
