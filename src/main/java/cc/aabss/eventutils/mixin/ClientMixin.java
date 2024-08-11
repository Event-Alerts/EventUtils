package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallback;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.text.Text.translatable;


@Mixin(MinecraftClient.class)
public class ClientMixin {
    @Inject(method = "run", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        if (!EventUtils.MOD.config.confirmWindowClose) return;
        final MinecraftClient client = MinecraftClient.getInstance();
        final long handle = client.getWindow().getHandle();
        final GLFWWindowCloseCallback callback = GLFW.glfwSetWindowCloseCallback(handle, win -> client.execute(() -> {
            GLFW.glfwSetWindowShouldClose(handle, false);
            final Screen current = client.currentScreen;
            client.setScreen(new ConfirmScreen(
                    result -> {
                        if (result) {
                            client.scheduleStop();
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
