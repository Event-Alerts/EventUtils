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

import java.util.UUID;


@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract UUID getUuid();
    @Shadow public abstract Text getName();
    @Shadow public abstract EntityType<?> getType();
    //? if >=1.21.11 {
    /*@Shadow public abstract Vec3d getSyncedPos();
    *///?} else {
    @Shadow public abstract Vec3d getPos();
    //?}

    @Inject(method = "spawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void spawnSprintingParticles(CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        final EntityType<?> type = getType();

        // Get position
        //? if >=1.21.11 {
        /*final Vec3d position = getSyncedPos();
        *///?} else {
        final Vec3d position = getPos();
        //?}

        if (((Object) this) instanceof ClientPlayerEntity player) {
            // Players
            if (!EventUtils.MOD.groupManager.isPlayerVisible(player.getGameProfile(), position)) ci.cancel();
        } else {
            // Entity
            if (!EventUtils.MOD.groupManager.isEntityVisible(type, position)) ci.cancel();
        }
    }
}
