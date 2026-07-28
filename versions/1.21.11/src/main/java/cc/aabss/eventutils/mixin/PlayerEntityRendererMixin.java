package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void updateRenderState(PlayerLikeEntity entity, PlayerEntityRenderState renderState, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        ((PlayerEntityRenderStateAccessor) renderState).eventutils$setRawName(new VersionedGameProfile(player.getGameProfile()).getName());
    }
}
