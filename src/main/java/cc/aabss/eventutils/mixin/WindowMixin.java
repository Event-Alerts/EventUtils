//? if >=26.3 {
/*package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "requestClose", at = @At("HEAD"), cancellable = true)
    private void onRequestClose(CallbackInfo ci) {
        if (!EventUtils.MOD.config.confirm_window_close) return;
        ci.cancel();
        EventUtils.confirmExit(() -> {});
    }
}
*///?}
