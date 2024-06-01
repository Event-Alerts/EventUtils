package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.HidePlayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void render(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        if (player.isMainPlayer() || !HidePlayers.HIDEPLAYERS){
            return;
        }
        if (EventUtils.WHITELISTED_PLAYERS.contains(player.getName().getString().toLowerCase())) {
            return;
        }
        Text p = player.getName();
        if (
                p.contains(Text.literal("[")) ||
                p.contains(Text.literal("]")) ||
                p.contains(Text.literal(" ")) ||
                p.contains(Text.literal("-"))
        ){
            return;
        }
        ci.cancel();
    }
}
