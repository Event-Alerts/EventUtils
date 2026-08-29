package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EventWrapper;
import cc.aabss.eventutils.versioning.VersionedGuiGraphics;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class EventInfoScreen extends Screen {
    private static final int BOX_WIDTH = 350;
    private static final int BOX_HEIGHT = 280;

    /**
     * {@link EAEvent} or {@link EAFamousEvent}
     */
    @NotNull private final EventWrapper event;

    public EventInfoScreen(@NotNull EventUtils mod, @NotNull EventWrapper event) {
        super(Component.translatable(mod.keybindManager.eventInfoKey.getName()));
        this.event = event;
    }

    @Override
    //? if >=26.1 {
    /*public void extractRenderState(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    *///?} else {
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //?}
        final VersionedGuiGraphics vGraphics = new VersionedGuiGraphics(graphics);

        final int boxX = (width - BOX_WIDTH) / 2;
        final int boxY = (height - BOX_HEIGHT) / 2;
        final int startX = boxX + (BOX_WIDTH / 2);

        // Draw box
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0x88000000);

        // Draw lines
        final Font textRenderer = Minecraft.getInstance().font;
        int startY = boxY + 5;
        for (final String line : event.toInfoScreenText()) {
            // Split long lines into multiple lines
            if (textRenderer.width(line) > BOX_WIDTH - 10) {
                final List<FormattedCharSequence> splitLines = textRenderer.split(FormattedText.of(line), BOX_WIDTH - 10);
                for (final FormattedCharSequence splitLine : splitLines) {
                    vGraphics.drawCenteredString(textRenderer, splitLine, startX, startY, 0xFFFFFFFF);
                    startY += 12;
                }
                continue;
            }

            // Draw line
            vGraphics.drawCenteredString(textRenderer, line, startX, startY, 0xFFFFFFFF);
            startY += 12;
        }
    }
}
