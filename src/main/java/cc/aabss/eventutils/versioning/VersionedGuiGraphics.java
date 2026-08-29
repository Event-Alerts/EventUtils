package cc.aabss.eventutils.versioning;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;


public class VersionedGuiGraphics {
    @NotNull private final GuiGraphics graphics;

    public VersionedGuiGraphics(@NotNull GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public void drawString(@NotNull Font font, @NotNull Component text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        /*graphics.text(
        *///? } else {
        graphics.drawString(
        //?}
                font, text, x, y, color);
    }

    public void drawString(@NotNull Font font, @NotNull FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        /*graphics.text(
        *///? } else {
        graphics.drawString(
        //?}
                font, text, x, y, color, shadow);
    }

    public void drawCenteredString(@NotNull Font font, @NotNull String text, int x, int y, int color) {
        //? if >=26.1 {
        /*graphics.centeredText(
        *///? } else {
        graphics.drawCenteredString(
        //?}
                font, text, x, y, color);
    }

    public void drawCenteredString(@NotNull Font font, @NotNull Component text, int x, int y, int color) {
        //? if >=26.1 {
        /*graphics.centeredText(
        *///? } else {
        graphics.drawCenteredString(
        //?}
                font, text, x, y, color);
    }

    public void drawCenteredString(@NotNull Font font, @NotNull FormattedCharSequence text, int x, int y, int color) {
        //? if >=26.1 {
        /*graphics.centeredText(
        *///? } else {
        graphics.drawCenteredString(
        //?}
                font, text, x, y, color);
    }
}
