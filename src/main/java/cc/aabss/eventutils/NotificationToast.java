package cc.aabss.eventutils;

import cc.aabss.eventutils.versioning.VersionedIdentifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
//? if >=1.21.6 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class NotificationToast implements Toast {
    //? if >=1.21.6 {
    /*@NotNull private static final Identifier TEXTURE
    *///?} else {
    @NotNull private static final ResourceLocation TEXTURE
    //?}
            = VersionedIdentifier.of("toast/notification");

    @NotNull private final Component title;
    @NotNull private final List<FormattedCharSequence> lines;
    private final int width;
    private final int height;
    public Visibility visibility;

    public NotificationToast(@NotNull Component title, @Nullable Component description, boolean displayEventInfoInstructions) {
        this.title = title;

        lines = new ArrayList<>();
        if (description != null) lines.add(description.getVisualOrderText());
        final FormattedCharSequence eventInfoInstructions = displayEventInfoInstructions ? Component.translatable("eventutils.event.toast.info_screen_button", EventUtils.MOD.keybindManager.eventInfoKey.getTranslatedKeyMessage()).getVisualOrderText() : null;
        if (eventInfoInstructions != null) lines.add(eventInfoInstructions);

        final Font textRenderer = Minecraft.getInstance().font;
        width = EventUtils.max(
                160,
                30 + textRenderer.width(title),
                eventInfoInstructions != null ? 30 + textRenderer.width(eventInfoInstructions) : 0,
                description != null ? 30 + textRenderer.width(description) : 0);
        height = 20 + Math.max(lines.size(), 1) * 12;
        visibility = Toast.Visibility.HIDE;
    }

    //? if >=1.21.2 {
    @Override @NotNull
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(@NotNull ToastManager toastManager, long startTime) {
        visibility = startTime >= Type.DEFAULT.displayDuration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
    //?}

    @Override @NotNull
    public NotificationToast.Type getToken() {
        return Type.DEFAULT;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    //? if <1.21.2 {
    /*public Toast.Visibility render(GuiGraphics drawContext, ToastManager manager, long startTime) {
    *///?} else {
    public void render(@NotNull GuiGraphics drawContext, @NotNull Font textRenderer, long startTime) {
    //?}
        if (width == 160 && lines.size() <= 1) {
            //? if <1.21.2 {
            /*drawContext.blitSprite(TEXTURE, 0, 0, width, height);
            *///?} else if >=1.21.6 {
            /*drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, width, height); // work on 1.21.4 and 1.21.5
            *///?} else {
            drawContext.blitSprite(RenderType::guiTextured, TEXTURE, 0, 0, width, height); // work on 1.21.4 and 1.21.5
            //?}
        } else {
            int minHeight = Math.min(4, height - 28);
            drawPart(drawContext, 0, 0, 28);
            for (int i = 28; i < height - minHeight; i += 10) drawPart(drawContext, 16, i, Math.min(16, height - i - minHeight));
            drawPart(drawContext, 32 - minHeight, height - minHeight, minHeight);
        }

        //? if <1.21.2
        /*final Font textRenderer = manager.getClient().font;*/
        if (lines.isEmpty()) {
            drawContext.drawString(textRenderer, title, 24, 12, Color.YELLOW.getRGB(), false);
        } else {
            drawContext.drawString(textRenderer, title, 24, 7, Color.YELLOW.getRGB(), false);
            for (int i = 0; i < lines.size(); ++i) drawContext.drawString(textRenderer, lines.get(i), 24, 18 + i * 12, -1, false);
        }
        //? if <1.21.2
        /*return startTime >= Type.DEFAULT.displayDuration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;*/
    }


    private void drawPart(@NotNull GuiGraphics context, int j, int k, int l) {
        final int m = j == 0 ? 20 : 5;
        final int n = Math.min(60, width - m);
        final int widthN = width - n;
        //? if <=1.21.1 {
        /*context.blitSprite(TEXTURE, 160, 32, 0, j, 0, k, m, l);
        for (int o = m; o < widthN; o += 64) context.blitSprite(TEXTURE, 160, 32, 32, j, o, k, Math.min(64, widthN - o), l);
        context.blitSprite(TEXTURE, 160, 32, 160 - n, j, widthN, k, n, l);
        *///?} else if >=1.21.6 {
        /*context.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 160, 32, 0, j, 0, k, m, l);
        for (int o = m; o < widthN; o += 64) context.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 160, 32, 32, j, o, k, Math.min(64, widthN - o), l);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 160, 32, 160 - n, j, widthN, k, n, l);
        *///?} else {
        context.blitSprite(RenderType::guiTextured, TEXTURE, 160, 32, 0, j, 0, k, m, l);
        for (int o = m; o < widthN; o += 64) context.blitSprite(RenderType::guiTextured, TEXTURE, 160, 32, 32, j, o, k, Math.min(64, widthN - o), l);
        context.blitSprite(RenderType::guiTextured, TEXTURE, 160, 32, 160 - n, j, widthN, k, n, l);
        //?}
    }

    @Environment(value = EnvType.CLIENT)
    public record Type(long displayDuration) {
        @NotNull public static final NotificationToast.Type DEFAULT = new NotificationToast.Type(10000L);
    }
}
