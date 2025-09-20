package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;


public class PriorityCmd {
    private static final int PLAYERS_PER_PAGE = 10;

    public static void priority(@NotNull CommandContext<FabricClientCommandSource> context, String name) {
        final MinecraftClient client = context.getSource().getClient();
        client.send(() -> {
            // Get names
            if (client.world == null || client.player == null) {
                context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.noplayer", EventUtils.ERROR_MESSAGE_PREFIX));
                return;
            }
            final List<String> namesSorted = client.world.getPlayers().stream()
                    .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                    .map(player -> player.getName().getString())
                    .filter(str -> !EventUtils.isNPC(str, true))
                    .toList();

            final String nameLower = name.toLowerCase();
            for (final String playerName : namesSorted) if (nameLower.equals(playerName.toLowerCase())) {
                if (playerName.equalsIgnoreCase(client.player.getName().getString())) {
                    context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.self", EventUtils.MESSAGE_PREFIX, "§6#" + (namesSorted.indexOf(playerName) + 1)));
                } else {
                    context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.player", EventUtils.MESSAGE_PREFIX, Text.literal(playerName).formatted(Formatting.YELLOW), "§6#" + (namesSorted.indexOf(playerName) + 1)));
                }

                return;
            }
            context.getSource().sendFeedback(Text.translatable("eventutils.command.priority.noplayer"));
        });
    }

    public static void priority(@NotNull CommandContext<FabricClientCommandSource> context, int page) {
        final FabricClientCommandSource source = context.getSource();
        final MinecraftClient client = source.getClient();
        client.send(() -> {
            // Get names
            if (client.world == null || client.player == null) {
                source.sendFeedback(Text.translatable("eventutils.command.prioritytop.emptypage"));
                return;
            }
            final List<String> namesSorted = client.world.getPlayers().stream()
                    .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                    .map(player -> player.getName().getString())
                    .filter(str -> !EventUtils.isNPC(str, true))
                    .toList();

            // Check page bounds
            final int totalPlayers = namesSorted.size();
            final int totalPages = (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE);
            if (page > totalPages || page < 1) {
                source.sendFeedback(Text.translatable("eventutils.command.prioritytop.notapage", EventUtils.ERROR_MESSAGE_PREFIX, "§f" + totalPages));
                return;
            }

            // Send page
            final int pageIndex = Math.max(0, page - 1);
            final int start = pageIndex * PLAYERS_PER_PAGE;
            final int end = Math.min(start + PLAYERS_PER_PAGE, totalPlayers);
            final String clientName = client.player.getName().getString().toLowerCase();
            final MutableText text = Text.translatable("eventutils.command.prioritytop.page", EventUtils.MESSAGE_PREFIX, "§6" + page, "§6" + totalPages);
            for (int i = start; i < end; i++) {
                final String name = namesSorted.get(i);
                final boolean isLocalPlayer = name.equalsIgnoreCase(clientName);
                final String boldModifier = isLocalPlayer ? "§l" : "";
                Style colorModifier;
                int placement = i + 1;

                colorModifier = switch (placement) {
                    case 1 -> Style.EMPTY.withColor(TextColor.fromRgb(0xFFEA5C));
                    case 2 -> Style.EMPTY.withColor(TextColor.fromRgb(0xC0C0C0));
                    case 3 -> Style.EMPTY.withColor(TextColor.fromRgb(0xA97142));
                    default -> {
                        if (isLocalPlayer) {
                            yield Style.EMPTY.withColor(TextColor.fromRgb(0x9AED47));
                        }
                        yield Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.WHITE));
                    }
                };

                final MutableText item = Text.literal(String.format("%s%d - %s\n", boldModifier, placement, isLocalPlayer ? name + " (You)" : name)).fillStyle(colorModifier);
                text.append(item);
            }
            source.sendFeedback(text);

            MutableText lastpage = Text.literal("");
            MutableText nextpage = Text.literal("");

            if (page > 1) {
                lastpage = Text.translatable(
                        "eventutils.command.prioritytop.lastpage",
                        page - 1
                ).setStyle(
                        //? if <=1.21.4 {
                        Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eventutils prioritytop " + (page - 1))
                        )
                        //?} else {
                        
                        /*Style.EMPTY.withClickEvent(
                                new ClickEvent.RunCommand("/eventutils prioritytop " + (page - 1))
                        )
                        
                        *///?}
                );
            }

            if (page + 1 <= totalPages) {
                nextpage = Text.translatable(
                        "eventutils.command.prioritytop.nextpage",
                        page + 1
                ).setStyle(
                        //? if <=1.21.4 {
                        Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eventutils prioritytop " + (page + 1))
                        )
                        //?} else {
                        
                        /*Style.EMPTY.withClickEvent(
                                new ClickEvent.RunCommand("/eventutils prioritytop " + (page + 1))
                        )
                        
                        *///?}
                );
            }

            if (!lastpage.getString().isEmpty() || !nextpage.getString().isEmpty()) {
                source.sendFeedback(Text.translatable("eventutils.command.prioritytop.pagebutton", EventUtils.MESSAGE_PREFIX, lastpage, nextpage));
            }
        });
    }
}
