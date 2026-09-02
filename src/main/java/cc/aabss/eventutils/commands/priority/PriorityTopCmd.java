package cc.aabss.eventutils.commands.priority;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.commands.EUCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class PriorityTopCmd extends EUCommand {
    private static final int PLAYERS_PER_PAGE = 10;

    public PriorityTopCmd(@NotNull EventUtils mod) {
        super(mod);
    }

    @Override @NotNull
    public LiteralCommandNode<FabricClientCommandSource> build() {
        return ClientCommandManager
                .literal("prioritytop")
                .then(ClientCommandManager.argument("page", IntegerArgumentType.integer())
                        .executes((context) -> {
                            execute(context, IntegerArgumentType.getInteger(context, "page"));
                            return 0;
                        }))
                .executes(context -> {
                    execute(context, 1);
                    return 0;
                })
                .build();
    }

    private static void execute(@NotNull CommandContext<FabricClientCommandSource> context, int page) {
        final FabricClientCommandSource source = context.getSource();
        final Minecraft client = source.getClient();
        client.execute(() -> {
            // Get names
            final List<String> namesSorted = PriorityCommon.getNamesSorted(client);
            if (namesSorted == null || client.player == null) {
                source.sendFeedback(Component.translatable("eventutils.command.prioritytop.emptypage"));
                return;
            }

            // Check page bounds
            final int totalPlayers = namesSorted.size();
            final int totalPages = (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE);
            if (page > totalPages || page < 1) {
                source.sendFeedback(Component.translatable("eventutils.command.prioritytop.notapage", EventUtils.ERROR_MESSAGE_PREFIX, "§f" + totalPages));
                return;
            }

            // Send page
            final int pageIndex = Math.max(0, page - 1);
            final int start = pageIndex * PLAYERS_PER_PAGE;
            final int end = Math.min(start + PLAYERS_PER_PAGE, totalPlayers);
            final String clientName = client.player.getName().getString().toLowerCase();
            final MutableComponent text = Component.translatable("eventutils.command.prioritytop.page", EventUtils.MESSAGE_PREFIX, "§6" + page, "§6" + totalPages);
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
                        yield Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.WHITE));
                    }
                };

                final MutableComponent item = Component.literal(String.format("%s%d - %s\n", boldModifier, placement, isLocalPlayer ? name + " (You)" : name)).withStyle(colorModifier);
                text.append(item);
            }
            source.sendFeedback(text);

            MutableComponent lastpage = Component.literal("");
            MutableComponent nextpage = Component.literal("");

            if (page > 1) {
                final String command = "/eventutils prioritytop " + (page - 1);
                lastpage = Component.translatable("eventutils.command.prioritytop.lastpage", page - 1).setStyle(Style.EMPTY.withClickEvent(
                        //? if <=1.21.4 {
                          new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
                        //?} else {
                        /*new ClickEvent.RunCommand(command)
                        *///?}
                ));
            }

            if (page + 1 <= totalPages) {
                nextpage = Component.translatable(
                        "eventutils.command.prioritytop.nextpage",
                        page + 1
                ).setStyle(
                        //? if <=1.21.4 {
                          Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eventutils prioritytop " + (page + 1)))
                        //?} else {
                        /*Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/eventutils prioritytop " + (page + 1)))
                        *///?}
                );
            }

            if (!lastpage.getString().isEmpty() || !nextpage.getString().isEmpty()) {
                source.sendFeedback(Component.translatable("eventutils.command.prioritytop.pagebutton", EventUtils.MESSAGE_PREFIX, lastpage, nextpage));
            }
        });
    }
}
