package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EventUtilsCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("eventutilsconfig")
                        .then((ClientCommandManager.argument("key", StringArgumentType.word())
                                .suggests((context, builder) -> builder
                                        .suggest("default-famous-ip")
                                        .suggest("auto-tp")
                                        .suggest("discord-rpc")
                                        .suggest("simple-queue-msg")
                                        .suggest("famous-event")
                                        .suggest("potential-famous-event")
                                        .suggest("money-event")
                                        .suggest("partner-event")
                                        .suggest("fun-event")
                                        .suggest("housing-event")
                                        .suggest("community-event")
                                        .suggest("civilization-event")
                                        .buildFuture())
                                .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            SuggestionsBuilder builder1 = builder.restart();
                                            if (!key.equalsIgnoreCase("default-famous-ip")) {
                                                builder1
                                                        .suggest("true")
                                                        .suggest("false");
                                            }
                                            return builder1.buildFuture();
                                        })
                                        .executes((context) -> run(context.getSource().getPlayer(), context.getInput()))))
                                .executes((context) -> run(context.getSource().getPlayer(), context.getInput()))));
    }

    public static int run(ClientPlayerEntity client, String command) {
        String[] split = command.split(" ");
        if (split.length != 1 && split.length != 2) {
            String key = split[1];
            String value = split[2];
            Field valueField;
            if (key.equalsIgnoreCase("default-famous-ip")) {
                try {
                    valueField = EventUtils.class.getDeclaredField(key.toUpperCase().replaceAll("-", "_"));
                    valueField.setAccessible(true);
                    valueField.set(new EventUtils(), value);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

                EventUtils.CONFIG.saveObject(key, value);
            } else {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    client.sendMessage(Text.literal("Invalid boolean. (true/false)").formatted(Formatting.RED));
                    return -1;
                }

                try {
                    valueField = EventUtils.class.getDeclaredField(key.toUpperCase().replaceAll("-", "_"));
                    valueField.setAccessible(true);
                    valueField.set(new EventUtils(), Boolean.valueOf(value));
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

                EventUtils.CONFIG.saveObject(key, Boolean.valueOf(value));
            }

            EventUtils.CONFIG.saveConfig(EventUtils.CONFIG.JSON);
            client.sendMessage(Text.literal("Set " + key + " to " + value + ".").formatted(Formatting.GREEN));
            return 1;
        } else {
            client.sendMessage(Text.literal("Usage: /" + split[0] + " <key> <value>").formatted(Formatting.RED));
            return -1;
        }
    }
}
