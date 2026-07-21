package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
//? if >=1.21.11 {
/*import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.entity.EntityType;
*///?} else {
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
//?}
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
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
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.11 {
    /*private void render(EntityRenderState renderState, CameraRenderState cameraRenderState, double x, double y, double z, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
    *///?} else {
    private <E extends Entity> void render(E entity, double x, double y, double z, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
    //?}
        // Get target position
        //? if >=1.21.11 {
        /*final Vec3d position = new Vec3d(renderState.x, renderState.y, renderState.z);
        *///?} else {
        final Vec3d position = entity.getSyncedPos();
        //?}

        // Player
        //? if >=1.21.11 {
        /*if (renderState instanceof PlayerEntityRenderState playerRenderState) {
            final String name = ((PlayerEntityRenderStateAccessor) playerRenderState).eventutils$getRawName();
        *///?} else {
        if (entity instanceof PlayerEntity player) {
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
