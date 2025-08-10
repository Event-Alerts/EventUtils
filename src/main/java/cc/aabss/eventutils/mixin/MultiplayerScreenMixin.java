package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {

    @Shadow private ServerList serverList;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Store reference to server list for EventServerManager
        if (EventUtils.MOD != null) {
            EventUtils.MOD.eventServerManager.setServerList(this.serverList);
        }
    }
}
