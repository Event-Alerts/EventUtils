package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.api.NotificationToast;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TestNotificationCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher){
        dispatcher.register(
                ClientCommandManager.literal("testnotification")
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
                    Text.literal("Usage: /testnotification <famous | potential | money | partner | fun | housing | community | civilization>").formatted(Formatting.RED));
            return -1;
        }
        String eventType = command.split(" ")[1];
        if (eventType.equalsIgnoreCase("famous")) {
            NotificationToast.addFamousEvent();
        } else if (eventType.equalsIgnoreCase("potential")) {
            NotificationToast.addPotentialFamousEvent();
        } else if (eventType.equalsIgnoreCase("money")) {
            NotificationToast.addMoneyEvent();
        } else if (eventType.equalsIgnoreCase("partner")) {
            NotificationToast.addPartnerEvent();
        } else if (eventType.equalsIgnoreCase("fun")) {
            NotificationToast.addFunEvent();
        } else if (eventType.equalsIgnoreCase("housing")) {
            NotificationToast.addHousingEvent();
        } else if (eventType.equalsIgnoreCase("community")) {
            NotificationToast.addCommunityEvent();
        } else if (eventType.equalsIgnoreCase("civilization")) {
            NotificationToast.addCivilizationEvent();
        } else {
            client.sendMessage(
                    Text.literal("Usage: /eventteleport <famous | potential_famous | money | partner | fun | housing | community | civilization>").formatted(Formatting.RED));
            return -1;
        }
        client.sendMessage(Text.literal("Sent test notification for " + eventType).formatted(Formatting.GOLD));
        return 1;
    }
}
