package cc.aabss.eventutils.plustag;

//? if >=1.21.6 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?} else if >=1.21.4 {
import net.minecraft.client.renderer.RenderType;
//?}
//? if >=1.21.6 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;


/**
 * Draws the plus tag icon (full texture, no slicing).
 * Expects 64x64 textures in assets/eventutils/textures/bee/
 */
public final class IconRenderer {
    private static final int TEX_SIZE = 64;

    private IconRenderer() {}

    /** Draw the icon at (x, y) with the given size (e.g. 8 for tab list). Samples full 64x64 texture, scaled to size. */
    public static void draw(@NotNull GuiGraphics context, @NotNull Object texture, int x, int y, int size) {
        //? if >=1.21.6 {
        /*context.blit(RenderPipelines.GUI_TEXTURED, (Identifier) texture, x, y, 0f, 0f, size, size, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        *///?} else if >=1.21.4 {
        context.blit(RenderType::guiTextured, (ResourceLocation) texture, x, y, 0f, 0f, size, size, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        //?} else {
        /*context.blit((ResourceLocation) texture, x, y, 0f, 0f, size, size, TEX_SIZE, TEX_SIZE);
        *///?}
    }
}
