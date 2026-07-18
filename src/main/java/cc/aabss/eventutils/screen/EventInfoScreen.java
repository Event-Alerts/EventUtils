package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EventWrapper;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class EventInfoScreen extends Screen {
    private static final int BOX_WIDTH = 350;
    private static final int BOX_HEIGHT = 280;

    /**
     * {@link EAEvent} or {@link EAFamousEvent}
     */
    @NotNull private final EventWrapper event;

    public EventInfoScreen(@NotNull EventWrapper event) {
        //? if >=1.21.11 {
        /*super(Text.translatable(EventUtils.MOD.keybindManager.eventInfoKey.getId()));
        *///?} else {
        super(Text.translatable(EventUtils.MOD.keybindManager.eventInfoKey.getTranslationKey()));
        //?}
        this.event = event;
    }

    @Override
    public void render(DrawContext drawContext, int i, int j, float f) {
        final int boxX = (width - BOX_WIDTH) / 2;
        final int boxY = (height - BOX_HEIGHT) / 2;
        final int startX = boxX + (BOX_WIDTH / 2);

        // Draw box
        drawContext.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0x88000000);

        // Draw lines
        final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int startY = boxY + 5;
        for (final String line : event.toInfoScreenText()) {
            // Split long lines into multiple lines
            if (textRenderer.getWidth(line) > BOX_WIDTH - 10) {
                final List<OrderedText> splitLines = textRenderer.wrapLines(StringVisitable.plain(line), BOX_WIDTH - 10);
                for (final OrderedText splitLine : splitLines) {
                    drawContext.drawCenteredTextWithShadow(textRenderer, splitLine, startX, startY, 0xFFFFFFFF);
                    startY += 12;
                }
                continue;
            }

            // Draw line
            drawContext.drawCenteredTextWithShadow(textRenderer, line, startX, startY, 0xFFFFFFFF);
            startY += 12;
        }
    }
}
