package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
//? if >=1.21.11 {
/*import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
*///?}
//? if >=1.21.3 {
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//?} else {
/*import net.minecraft.client.network.AbstractClientPlayerEntity;
*///?}
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    //? if >=1.21.11 {
    /*@Inject(at = @At("HEAD"), method = "renderLabelIfPresent", cancellable = true)
    public void renderLabelIfPresent(PlayerEntityRenderState player, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!EventUtils.MOD.isInHidePlayersMode()) return;
        final ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer == null) return;
        final Text nameText = player.playerName;
        if (nameText == null) return;
        final String name = nameText.getString().toLowerCase();
        if (name.equals(clientPlayer.getName().getString().toLowerCase())) return;
        if (!EventUtils.MOD.isPlayerVisible(name)) {
            ci.cancel();
            return;
        }
        if (!EventUtils.MOD.shouldShowNametagFor(name)) {
            ci.cancel();
            return;
        }
        if (EventUtils.MOD.config.hidePlayersRadius == 0) return;
        final Vec3d playerPos = new Vec3d(player.x, player.y, player.z);
        if (clientPlayer.getSyncedPos().distanceTo(playerPos) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
    *///?} else {
    @Inject(at = {@At("HEAD")}, method = "renderLabelIfPresent*", cancellable = true)
    //? if <=1.20.4 {
    /*public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
     *///?} else if <1.21.3 {
    /*public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
    *///?} else {
    public void renderLabelIfPresent(PlayerEntityRenderState player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
    //?}
        if (!EventUtils.MOD.isInHidePlayersMode()) return;
        final ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer == null) return;

        // Get player name
        //? if <1.21.3 {
        /*final Text nameText = player.getName();
        *///?} else {
        final Text nameText = player.playerName;
        if (nameText == null) return;
        //?}
        final String name = nameText.getString().toLowerCase();

        // Check if main player
        //? if <1.21.3 {
        /*if (player.isMainPlayer()) return;
        *///?} else {
        if (name.equals(clientPlayer.getName().getString().toLowerCase())) return;
        //?}

        // Not visible in current view mode -> hide nametag
        if (!EventUtils.MOD.isPlayerVisible(name)) {
            ci.cancel();
            return;
        }
        // Visible: respect per-group nametag setting
        if (!EventUtils.MOD.shouldShowNametagFor(name)) {
            ci.cancel();
            return;
        }

        // Any radius
        if (EventUtils.MOD.config.hidePlayersRadius == 0) return;

        // Get player position
        //? if <1.21.3 {
        /*final Vec3d playerPos = player.getPos();
        *///?} else {
        final Vec3d playerPos = new Vec3d(player.x, player.y, player.z);
        //?}

        // Radius-specific
        if (clientPlayer.getPos().distanceTo(playerPos) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
    //?}
}
