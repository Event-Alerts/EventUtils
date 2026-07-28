package cc.aabss.eventutils.plustag;

import net.minecraft.client.gui.DrawContext;
//? if >=1.21.6 {
/*import net.minecraft.client.gl.RenderPipelines;
*///?} else if >=1.21.4 {
import net.minecraft.client.render.RenderLayer;
//?}
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;


/**
 * Draws the plus tag icon (full texture, no slicing).
 * Expects 64x64 textures in assets/eventutils/textures/bee/
 */
public final class IconRenderer {
    private static final int TEX_SIZE = 64;

    private IconRenderer() {}

    /** Draw the icon at (x, y) with the given size (e.g. 8 for tab list). Samples full 64x64 texture, scaled to size. */
    public static void draw(@NotNull DrawContext context, @NotNull Identifier texture, int x, int y, int size) {
        //? if >=1.21.6 {
        /*context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        *///?} else if >=1.21.4 {
        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0f, 0f, size, size, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        //?} else {
        //context.drawTexture(texture, x, y, 0f, 0f, size, size, TEX_SIZE, TEX_SIZE);
        //?}
    }
}
