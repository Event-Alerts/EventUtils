package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void render(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!EventUtils.MOD.hidePlayers) return;

        // Non-players (mob)
        if (!(entity instanceof PlayerEntity player)) {
            if (EventUtils.MOD.config.hiddenEntityTypes.contains(entity.getType())) {
                if (EventUtils.MOD.config.hidePlayersRadius == 1) {
                    ci.cancel();
                } else {
                    if (entity.getPos().distanceTo(entity.getPos()) <= EventUtils.MOD.config.hidePlayersRadius) {
                        ci.cancel();
                    }
                }
            }
            return;
        }

        // Players
        if (player.isMainPlayer()) return; // Check if self
        final String name = player.getName().getString().toLowerCase();
        if (!EventUtils.MOD.config.whitelistedPlayers.contains(name) // Check if player whitelisted
                && !name.contains("[") && !name.contains("]") && !name.contains(" ") && !name.contains("-")) { // Check if player is an NPC
            if (EventUtils.MOD.config.hidePlayersRadius == 1) {
                ci.cancel();
            } else {
                if (entity.getPos().distanceTo(player.getPos()) <= EventUtils.MOD.config.hidePlayersRadius) {
                    ci.cancel();
                }
            }
        }
    }
}
