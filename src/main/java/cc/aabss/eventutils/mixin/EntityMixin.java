package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;


@Mixin(Entity.class) @MixinEnvironment(type = MixinEnvironment.Env.MAIN)
public abstract class EntityMixin {
    @Shadow public abstract UUID getUUID();
    @Shadow public abstract Component getName();
    @Shadow public abstract EntityType<?> getType();
    //? if >=1.21.11 {
    /*@Shadow public abstract Vec3 trackingPosition();
    *///?} else {
    @Shadow public abstract Vec3 position();
    //?}

    //TODO inject into canSpawnSprintParticle instead
    @Inject(method = "spawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void spawnSprintingParticles(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;
        final EntityType<?> type = getType();

        // Get position
        //? if >=1.21.11 {
        /*final Vec3d position = trackingPosition();
        *///?} else {
        final Vec3 position = position();
        //?}

        if (((Object) this) instanceof LocalPlayer player) {
            // Players
            if (!EventUtils.MOD.groupManager.isPlayerVisible(player.getGameProfile(), position)) ci.cancel();
        } else {
            // Entity
            if (!EventUtils.MOD.groupManager.isEntityVisible(type, position)) ci.cancel();
        }
    }
}
