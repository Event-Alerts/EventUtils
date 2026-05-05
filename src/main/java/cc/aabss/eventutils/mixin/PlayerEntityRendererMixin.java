package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(at = {@At("HEAD")}, method = "renderLabelIfPresent*", cancellable = true)
    public void renderLabelIfPresent(PlayerEntityRenderState player, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!EventUtils.MOD.hidePlayers) return;
        final ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer == null) return;
        if (player.id == clientPlayer.getId()) return;
        final Text nameText = player.playerName != null ? player.playerName : player.displayName;
        if (nameText == null) return;
        final String name = nameText.getString().toLowerCase().replaceAll("§.", "").trim();
        if (EventUtils.MOD.config.whitelistedPlayers.contains(name) || EventUtils.isNPC(name)) return;
        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }
        final Vec3d playerPos = new Vec3d(player.x, player.y, player.z);
        if (clientPlayer.getEntityPos().distanceTo(playerPos) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
}
