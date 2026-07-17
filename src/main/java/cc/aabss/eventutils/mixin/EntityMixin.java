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
    //? if >=1.21.11 {
    /*@Shadow public abstract Vec3d getSyncedPos();
    *///?} else {
    @Shadow public abstract Vec3d getPos();
    //?}
    @Shadow public abstract Text getName();

    @Inject(method = "spawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void spawnSprintingParticles(CallbackInfo ci) {
        if (EventUtils.MOD.isHidePlayersRevealed()) return;
        final ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        if (mainPlayer == null) return;
        final EntityType<?> type = getType();

        if (type == EntityType.PLAYER) {
            // Players
            final String name = getName().getString().toLowerCase();
            if (mainPlayer.getName().getString().toLowerCase().equals(name) || EventUtils.MOD.isPlayerVisible(name)) return;
        } else {
            // Non-players (mob)
            if (!EventUtils.MOD.config.hiddenEntityTypes.contains(type)) return;
        }

        // Any radius
        if (EventUtils.MOD.config.hidePlayersRadius == 0) {
            ci.cancel();
            return;
        }

        // Get distance to entity
        //? if >=1.21.11 {
        /*final double distance = mainPlayer.getSyncedPos().distanceTo(getSyncedPos());
        *///?} else {
        final double distance = mainPlayer.getPos().distanceTo(getPos());
        //?}

        // Specific radius
        if (distance <= EventUtils.MOD.config.hidePlayersRadius) ci.cancel();
    }
}
