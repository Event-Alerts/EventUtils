package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

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
            if (EventUtils.MOD.config.hiddenEntityTypes.contains(entity.getType())) ci.cancel();
            return;
        }

        if (player.isMainPlayer() || !EventUtils.MOD.hidePlayers) return;
        if (EventUtils.MOD.config.whitelistedPlayers.contains(player.getName().getString().toLowerCase())) return;

        final Text name = player.getName();
        if (name.contains(Text.literal("["))
                || name.contains(Text.literal("]"))
                || name.contains(Text.literal(" "))
                || name.contains(Text.literal("-"))) return;
        ci.cancel();
    }
}
