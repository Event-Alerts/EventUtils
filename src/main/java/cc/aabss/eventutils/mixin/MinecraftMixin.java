package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.chat.Component.translatable;


@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "run", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        if (!EventUtils.MOD.config.confirm_window_close) return;
        final Minecraft client = Minecraft.getInstance();
        //? if >=1.21.11 {
        /*final long handle = client.getWindow().handle();
        *///?} else {
        final long handle = client.getWindow().getWindow();
        //?}
        final GLFWWindowCloseCallback callback = GLFW.glfwSetWindowCloseCallback(handle, win -> client.execute(() -> {
            GLFW.glfwSetWindowShouldClose(handle, false);
            final Screen current = client.screen;
            client.setScreen(new ConfirmScreen(
                    result -> {
                        if (result) {
                            client.stop();
                            return;
                        }
                        GLFW.glfwSetWindowShouldClose(handle, false);
                        client.setScreen(current);
                    },
                    translatable("eventutils.confirm_exit.title"),
                    translatable("eventutils.confirm_exit.message")));
        }));
        if (callback != null) callback.free();
    }
}
