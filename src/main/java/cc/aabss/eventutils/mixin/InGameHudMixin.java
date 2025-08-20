package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.skins.SkinFinderOverlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void eventutils$hideCrosshair(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo ci) {
        if (SkinFinderOverlay.isOpen()) ci.cancel();
    }
}


