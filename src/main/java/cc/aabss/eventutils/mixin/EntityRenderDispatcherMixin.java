package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityRenderManager.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(EntityRenderState entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
        if (!EventUtils.MOD.hidePlayers) return;

        if (entityRenderState instanceof PlayerEntityRenderState playerRenderState) {
            final ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
            if (clientPlayer == null) return;
            if (playerRenderState.id == clientPlayer.getId()) return;
            final net.minecraft.text.Text nameText = playerRenderState.playerName != null ? playerRenderState.playerName : playerRenderState.displayName;
            if (nameText != null) {
                final String name = nameText.getString().toLowerCase().replaceAll("§.", "").trim();
                if (EventUtils.MOD.config.whitelistedPlayers.contains(name) || EventUtils.isNPC(name)) return;
            }
        } else {
            if (!EventUtils.MOD.config.hiddenEntityTypes.contains(entityRenderState.entityType)) return;
        }

        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        final Vec3d entityPos = new Vec3d(entityRenderState.x, entityRenderState.y, entityRenderState.z);
        if (mainPlayer != null && mainPlayer.getEntityPos().distanceTo(entityPos) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
}
