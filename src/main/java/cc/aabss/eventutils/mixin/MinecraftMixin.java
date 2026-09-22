//? if <26.3 {
package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "run", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        if (!EventUtils.MOD.config.confirm_window_close) return;

        // Get handle
        final Window window = Minecraft.getInstance().getWindow();
        //? if >=1.21.11 {
        /*final long handle = window.handle();
        *///?} else {
        final long handle = window.getWindow();
        //?}

        // Create callback
        final GLFWWindowCloseCallback callback = GLFW.glfwSetWindowCloseCallback(
                handle,
                win -> EventUtils.confirmExit(() -> GLFW.glfwSetWindowShouldClose(handle, false)));
        if (callback != null) callback.free();
    }
}
//?}
