package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.api.websocket.EventListener;
import cc.aabss.eventutils.config.EventUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EventTeleportCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher){
        dispatcher.register(
                ClientCommandManager.literal("eventteleport")
                        .then(ClientCommandManager.argument("eventType", StringArgumentType.greedyString())
                                .suggests((context, builder) -> builder
                                        .suggest("famous")
                                        .suggest("potential")
                                        .suggest("money")
                                        .suggest("partner")
                                        .suggest("fun")
                                        .suggest("housing")
                                        .suggest("community")
                                        .suggest("civilization").buildFuture())
                                .executes(context -> run(context.getSource().getPlayer(), context.getInput()))
                        ));
        dispatcher.register(
                ClientCommandManager.literal("eventutils")
                        .then(ClientCommandManager.argument("eventType", StringArgumentType.greedyString())
                                .suggests((context, builder) -> builder
                                        .suggest("famous")
                                        .suggest("potential")
                                        .suggest("money")
                                        .suggest("partner")
                                        .suggest("fun")
                                        .suggest("housing")
                                        .suggest("community")
                                        .suggest("civilization").buildFuture())
                                .executes(context -> run(context.getSource().getPlayer(), context.getInput()))
                        ));
    }

    public static int run(ClientPlayerEntity client, String command){
        if (command.split(" ").length == 1) {
            client.sendMessage(
                    Text.literal("Usage: /" + command.split(" ")[0] + " <famous | potential | money | partner | fun | housing | community | civilization>").formatted(Formatting.RED));
            return -1;
        }
        String eventType = command.split(" ")[1];
        if (eventType.equalsIgnoreCase("famous")) {
            if (!EventUtils.FAMOUS_EVENT) {
                client.sendMessage(Text.literal("Famous events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_FAMOUS_IP.isBlank()) {
                client.sendMessage(Text.literal("No famous event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_FAMOUS_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("potential")) {
            if (!EventUtils.POTENTIAL_FAMOUS_EVENT) {
                client.sendMessage(Text.literal("Potential famous events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_POTENTIAL_FAMOUS_IP.isBlank()) {
                client.sendMessage(Text.literal("No potential famous event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_POTENTIAL_FAMOUS_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("money")) {
            if (!EventUtils.MONEY_EVENT) {
                client.sendMessage(Text.literal("Money events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_MONEY_IP.isBlank()) {
                client.sendMessage(Text.literal("No money event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_MONEY_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("partner")) {
            if (!EventUtils.PARTNER_EVENT) {
                client.sendMessage(Text.literal("Partner events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_PARTNER_IP.isBlank()) {
                client.sendMessage(Text.literal("No partner event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_PARTNER_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("fun")) {
            if (!EventUtils.FUN_EVENT) {
                client.sendMessage(Text.literal("Fun events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_FUN_IP.isBlank()) {
                client.sendMessage(Text.literal("No fun event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_FUN_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("housing")) {
            if (!EventUtils.HOUSING_EVENT) {
                client.sendMessage(Text.literal("Housing events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_HOUSING_IP.isBlank()) {
                client.sendMessage(Text.literal("No housing event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_HOUSING_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("community")) {
            if (!EventUtils.COMMUNITY_EVENT) {
                client.sendMessage(Text.literal("Community events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_COMMUNITY_IP.isBlank()) {
                client.sendMessage(Text.literal("No community event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_COMMUNITY_IP);
                return 1;
            }
        } else if (eventType.equalsIgnoreCase("civilization")) {
            if (!EventUtils.CIVILIZATION_EVENT) {
                client.sendMessage(Text.literal("Civilization events are disabled.").formatted(Formatting.RED));
                return -1;
            } else if (EventListener.LAST_CIVILIZATION_IP.isBlank()) {
                client.sendMessage(Text.literal("No civilization event.").formatted(Formatting.RED));
                return -1;
            } else {
                EventUtil.connect(EventListener.LAST_CIVILIZATION_IP);
                return 1;
            }
        } else {
            client.sendMessage(
                    Text.literal("Usage: /" + command.split(" ")[0] + " <famous | potential | money | partner | fun | housing | community | civilization>").formatted(Formatting.RED));
            return -1;
        }
    }
}
