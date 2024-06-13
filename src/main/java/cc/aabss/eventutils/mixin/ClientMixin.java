package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ClientMixin {
    @Inject(method = "run", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        if (EventUtils.CONFIRM_WINDOW_CLOSE) {
            MinecraftClient client = MinecraftClient.getInstance();
            Window window = client.getWindow();
            GLFW.glfwSetWindowCloseCallback(window.getHandle(), (win) -> client.execute(() -> {
                GLFW.glfwSetWindowShouldClose(window.getHandle(), false);
                Screen current = client.currentScreen;
                ConfirmScreen confirmScreen = new ConfirmScreen(
                        result -> {
                            if (result) {
                                client.scheduleStop();
                            } else {
                                GLFW.glfwSetWindowShouldClose(window.getHandle(), false);
                                client.setScreen(current);
                            }
                        },
                        Text.of("Confirm Exit"),
                        Text.of("Are you sure you want to exit the game?")
                );
                client.setScreen(confirmScreen);
            }));
        }
    }
}
