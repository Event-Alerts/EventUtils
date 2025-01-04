package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
//? if <1.21.3 {
/*import net.minecraft.client.network.AbstractClientPlayerEntity;
*///?} else {
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//?}

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(at = {@At("HEAD")}, method = "renderLabelIfPresent*", cancellable = true)
    //? if <=1.20.4 {
    /*public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
     *///?} else if <1.21.3 {
    /*public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
    *///?} else {
    public void renderLabelIfPresent(PlayerEntityRenderState player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
    //?}
        if (!EventUtils.MOD.hidePlayers) return;
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

        // Checks
        if (EventUtils.MOD.config.whitelistedPlayers.contains(name) || EventUtils.isNPC(name)) return;

        // Any radius
        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        // Get player position
        //? if <1.21.3 {
        /*final Vec3d playerPos = player.getPos();
        *///?} else {
        final Vec3d playerPos = new Vec3d(player.x, player.y, player.z);
        //?}

        // Radius-specific
        if (clientPlayer.getPos().distanceTo(playerPos) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
}
