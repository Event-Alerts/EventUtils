package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void extractRenderState(Avatar entity, AvatarRenderState renderState, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        ((PlayerEntityRenderStateAccessor) renderState).eventutils$setRawName(new VersionedGameProfile(player.getGameProfile()).getName());
    }
}
