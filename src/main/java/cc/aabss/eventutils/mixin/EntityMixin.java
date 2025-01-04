package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract EntityType<?> getType();
    @Shadow public abstract Vec3d getPos();
    @Shadow public abstract Text getName();

    @Inject(method = "spawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void spawnSprintingParticles(CallbackInfo ci) {
        if (!EventUtils.MOD.hidePlayers) return;
        final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        if (mainPlayer == null) return;
        final EntityType<?> type = getType();

        if (type == EntityType.PLAYER) {
            // Players
            final String name = getName().getString().toLowerCase();
            if (mainPlayer.getName().getString().toLowerCase().equals(name) || EventUtils.MOD.config.whitelistedPlayers.contains(name) || EventUtils.isNPC(name)) return;
        } else {
            // Non-players (mob)
            if (!EventUtils.MOD.config.hiddenEntityTypes.contains(type)) return;
        }

        // Any radius
        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        // Specific radius
        if (mainPlayer.getPos().distanceTo(getPos()) <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
}
