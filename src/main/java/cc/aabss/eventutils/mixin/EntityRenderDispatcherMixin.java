package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
//? if >=1.21.11 {
/*import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.util.math.Vec3d;
*///?} else {
import net.minecraft.client.render.entity.EntityRenderDispatcher;
//?}
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


//? if >=1.21.11 {
/*@Mixin(EntityRenderManager.class)
*///?} else {
@Mixin(EntityRenderDispatcher.class)
//?}
public class EntityRenderDispatcherMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(EntityRenderState renderState, CameraRenderState cameraRenderState, double x, double y, double z, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
        if (!EventUtils.MOD.isInHidePlayersMode()) return;

        if (renderState.entityType == EntityType.PLAYER) {
            final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
            if (renderState.displayName == null) return;
            if (mainPlayer != null && mainPlayer.getName().getString().equalsIgnoreCase(renderState.displayName.getString())) return;
            final String name = renderState.displayName.getString().toLowerCase();
            if (EventUtils.MOD.isPlayerVisible(name)) return;
        } else {
            if (!EventUtils.MOD.config.hiddenEntityTypes.contains(renderState.entityType)) return;
        }

        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        if (mainPlayer != null) {
            final Vec3d entityPos = new Vec3d(renderState.x, renderState.y, renderState.z);
            if (mainPlayer.getSyncedPos().distanceTo(entityPos) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void render(E entity, double x, double y, double z, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (!EventUtils.MOD.isInHidePlayersMode()) return;

        if (entity instanceof PlayerEntity player) {
            if (player.isMainPlayer()) return;
            final String name = player.getName().getString().toLowerCase();
            if (EventUtils.MOD.isPlayerVisible(name)) return;
        } else {
            if (!EventUtils.MOD.config.hiddenEntityTypes.contains(entity.getType())) return;
        }

        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        if (mainPlayer != null && mainPlayer.getPos().distanceTo(entity.getPos()) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
    //?}
}
