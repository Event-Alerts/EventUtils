package cc.aabss.eventutils;

import com.google.common.collect.ImmutableList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

import static cc.aabss.eventutils.EventUtils.eventInfoKey;

public class NotificationToast implements Toast {
    @NotNull private static final Identifier TEXTURE = Identifier.of("eventutils", "toast/notification");

    @NotNull private final Text title;
    @NotNull private final List<OrderedText> lines;
    private final int width;
    private final int height;
    public Visibility visibility;

    public NotificationToast(@NotNull Text title, @NotNull Text description) {
        this.title = title;
        this.lines = ImmutableList.of(description.asOrderedText(),
                Text.literal("Click ")
                        .append(Text.translatable(eventInfoKey.getBoundKeyTranslationKey()))
                        .append(Text.literal(" to view info."))
                        .asOrderedText()
        );
        final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        this.width = Math.max(160, 30 + Math.max(textRenderer.getWidth(title), textRenderer.getWidth(description)));
        this.height = 20 + Math.max(lines.size(), 1) * 12;
        this.visibility = Visibility.HIDE;
    }

    //? if >=1.21.2 {
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager toastManager, long startTime) {
        visibility = startTime >= Type.DEFAULT.displayDuration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
    //?}

    @Override @NotNull
    public NotificationToast.Type getType() {
        return Type.DEFAULT;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    //? if >=1.21.2 {
    public void draw(DrawContext drawContext, TextRenderer textRenderer, long startTime) {
    //?} else
    /*public Toast.Visibility draw(DrawContext drawContext, ToastManager manager, long startTime) {*/
        if (width == 160 && lines.size() <= 1) {
            //? if <=1.20.1 {
            /*drawContext.drawTexture(TEXTURE, 0, 0, 0, 0, width, height);
            *///?} else {
            drawContext.drawGuiTexture(/*? if >=1.21.2 {*/RenderLayer::getGuiTextured, /*?}*/TEXTURE, 0, 0, width, height);
            //?}
        } else {
            int minHeight = Math.min(4, height - 28);
            drawPart(drawContext, 0, 0, 28);
            for (int i = 28; i < height - minHeight; i += 10) drawPart(drawContext, 16, i, Math.min(16, height - i - minHeight));
            drawPart(drawContext, 32 - minHeight, height - minHeight, minHeight);
        }

        //? if <1.21.2
        /*final TextRenderer textRenderer = manager.getClient().textRenderer;*/
        if (lines.isEmpty()) {
            drawContext.drawText(textRenderer, title, 24, 12, Color.YELLOW.getRGB(), false);
        } else {
            drawContext.drawText(textRenderer, title, 24, 7, Color.YELLOW.getRGB(), false);
            for (int i = 0; i < lines.size(); ++i) drawContext.drawText(textRenderer, lines.get(i), 24, 18 + i * 12, -1, false);
        }
        //? if <1.21.2
        /*return startTime >= Type.DEFAULT.displayDuration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;*/
    }


    private void drawPart(@NotNull DrawContext context, int j, int k, int l) {
        final int m = j == 0 ? 20 : 5;
        final int n = Math.min(60, width - m);
        final int widthN = width - n;
        //? if <=1.20.1 {
        /*context.drawTexture(TEXTURE, 160, 32, 0, 0, 0, j, 0, k, m, l);
        for (int o = m; o < widthN; o += 64) context.drawTexture(TEXTURE, 160, 32, 0, 0, 32, j, o, k, Math.min(64, widthN - o), l);
        context.drawTexture(TEXTURE, 160, 32, 0, 0, 160 - n, j, widthN, k, n, l);
        *///?} else if <=1.21.1 {
        /*context.drawGuiTexture(TEXTURE, 160, 32, 0, j, 0, k, m, l);
        for (int o = m; o < widthN; o += 64) context.drawGuiTexture(TEXTURE, 160, 32, 32, j, o, k, Math.min(64, widthN - o), l);
        context.drawGuiTexture(TEXTURE, 160, 32, 160 - n, j, widthN, k, n, l);
        *///?} else {
        context.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 160, 32, 0, j, 0, k, m, l);
        for (int o = m; o < widthN; o += 64) context.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 160, 32, 32, j, o, k, Math.min(64, widthN - o), l);
        context.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 160, 32, 160 - n, j, widthN, k, n, l);
        //?}
    }

    @Environment(value = EnvType.CLIENT)
    public record Type(long displayDuration) {
        @NotNull public static final NotificationToast.Type DEFAULT = new NotificationToast.Type(10000L);
    }
}
