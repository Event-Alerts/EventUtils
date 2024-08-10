package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.ConnectUtility;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;


public class EventTeleportCmd extends EventCommand {
    public EventTeleportCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @NotNull
    protected Set<String> getAliases() {
        return Set.of(
                "eventutils",
                "eventtp",
                "evtp");
    }

    @NotNull
    protected Collection<ArgumentBuilder<FabricClientCommandSource, ? extends ArgumentBuilder<FabricClientCommandSource, ?>>> getArguments() {
        return Set.of(ClientCommandManager
                .argument("eventType", StringArgumentType.string())
                        .executes(this::run)
                .suggests((context, builder) -> builder
                        .suggest("famous")
                        .suggest("potential")
                        .suggest("money")
                        .suggest("partner")
                        .suggest("fun")
                        .suggest("housing")
                        .suggest("community")
                        .suggest("civilization").buildFuture()));
    }

    @Override
    protected int run(@NotNull CommandContext<FabricClientCommandSource> context) {
        final ClientPlayerEntity client = context.getSource().getPlayer();

        // Get event type
        if (context.getInput().split(" ").length <= 1){
            client.sendMessage(Text.literal("Usage: /" + context.getRootNode().getName() + " <famous|potential_famous|money|partner|fun|housing|community|civilization>").formatted(Formatting.RED));
            return 0;
        }
        final String eventTypeArg = context.getArgument("eventType", String.class);
        final EventType eventType = EventType.fromString(eventTypeArg);
        if (eventType == null) {
            client.sendMessage(Text.literal("Usage: /" + context.getRootNode().getName() + " <famous|potential_famous|money|partner|fun|housing|community|civilization>").formatted(Formatting.RED));
            return 0;
        }

        // Get last IP
        final String lastIP = mod.lastIps.get(eventType);
        if (lastIP == null) {
            client.sendMessage(Text.literal("No " + eventTypeArg + " event found!").formatted(Formatting.RED));
            return 0;
        }

        // Connect to last IP
        ConnectUtility.connect(lastIP);
        return 0;
    }
}
