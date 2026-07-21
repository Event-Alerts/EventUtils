package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
//? if >=1.21.11 {
/*import cc.aabss.eventutils.versioning.VersionedGameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.entity.PlayerLikeEntity;
*///?} else {
import net.minecraft.text.Text;
//?}
//? if >=1.21.3 {
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//?} else {
//import net.minecraft.client.network.AbstractClientPlayerEntity;
//?}
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void updateRenderState(PlayerLikeEntity entity, PlayerEntityRenderState renderState, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        ((PlayerEntityRenderStateAccessor) renderState).eventutils$setRawName(new VersionedGameProfile(player.getGameProfile()).getName());
    }
    *///?}

    @Inject(at = {@At("HEAD")}, method = "renderLabelIfPresent*", cancellable = true)
    //? if >=1.21.11 {
    /*public void renderLabelIfPresent(PlayerEntityRenderState player, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
    *///?} else if >=1.21.4 {
    public void renderLabelIfPresent(PlayerEntityRenderState player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
    //?} else if >1.20.4 {
    //public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
    //?} else {
    //public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
    //?}
        if (MinecraftClient.getInstance().player == null) return;

        // Get player name
        //? if >=1.21.4 {
        final String name = ((PlayerEntityRenderStateAccessor) player).eventutils$getRawName();
        //?} else {
        //final String name = player.getName().getString();
        //?}

        // Get player position
        //? if >=1.21.4 {
        final Vec3d position = new Vec3d(player.x, player.y, player.z);
        //?} else {
        //final Vec3d position = player.getPos();
        //?}

        // Check if nametag is visible
        if (!EventUtils.MOD.groupManager.isNametagVisible(name, position)) ci.cancel();
    }
}
