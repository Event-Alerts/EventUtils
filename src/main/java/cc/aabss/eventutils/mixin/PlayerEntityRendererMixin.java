package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(at = {@At("HEAD")}, method = "renderLabelIfPresent*", cancellable = true)
    public void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
        if (player.isMainPlayer()) return;
        String name = player.getName().getString().toLowerCase();
        if (EventUtils.MOD.config.whitelistedPlayers.contains(name) &&
                EventUtils.MOD.hidePlayers &&
                (!name.contains("[") && !name.contains("]") && !name.contains(" ") && !name.contains("-"))) {
            ci.cancel();
        }
    }
}
