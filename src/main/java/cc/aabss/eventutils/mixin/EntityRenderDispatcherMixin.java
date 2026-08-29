package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.21.11 {
/*import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
//?}
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(
            //? if >=1.21.11 {
            /*method = "submit*",
            *///?} else {
            method = "render*",
            //?}
            at = @At("HEAD"),
            cancellable = true)
    //? if >=1.21.11 {
    /*private void render(EntityRenderState renderState, CameraRenderState cameraRenderState, double x, double y, double z, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, CallbackInfo ci) {
    *///?} else if >1.21.4 {
    /*private <E extends Entity> void render(E entity, double x, double y, double z, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
    *///?} else {
    private <E extends Entity> void render(E entity, double x, double y, double z, float tickDelta, float h, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
    //?}
        // Get target position
        //? if >=1.21.11 {
        /*final Vec3 position = new Vec3(renderState.x, renderState.y, renderState.z);
        *///?} else {
        final Vec3 position = entity.trackingPosition();
        //?}

        // Player
        //? if >=1.21.11 {
        /*if (renderState instanceof AvatarRenderState playerRenderState) {
            final String name = ((PlayerEntityRenderStateAccessor) playerRenderState).eventutils$getRawName();
        *///?} else {
        if (entity instanceof Player player) {
            final String name = player.getName().getString();
            //?}

            if (!EventUtils.MOD.groupManager.isPlayerVisible(name, position)) ci.cancel();
        } else {
            // Entity
            //? if >=1.21.11 {
            /*final EntityType<?> entityType = renderState.entityType;
            *///?} else {
            final EntityType<?> entityType = entity.getType();
            //?}

            if (!EventUtils.MOD.groupManager.isEntityVisible(entityType, position)) ci.cancel();
        }
    }
}
