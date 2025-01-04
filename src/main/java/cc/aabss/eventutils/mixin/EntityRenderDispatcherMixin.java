package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
    //? if <=1.21.1 {
    /*private <E extends Entity> void render(Entity entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
    *///?} else {
    private <E extends Entity> void render(E entity, double x, double y, double z, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
    //?}
        if (!EventUtils.MOD.hidePlayers) return;

        if (entity instanceof PlayerEntity player) {
            // Players
            if (player.isMainPlayer()) return;
            final String name = player.getName().getString().toLowerCase();
            if (EventUtils.MOD.config.whitelistedPlayers.contains(name) || EventUtils.isNPC(name)) return;
        } else {
            // Non-players (mob)
            if (!EventUtils.MOD.config.hiddenEntityTypes.contains(entity.getType())) return;
        }

        // Any radius
        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        // Specific radius
        final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        if (mainPlayer != null && mainPlayer.getPos().distanceTo(entity.getPos()) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
}
