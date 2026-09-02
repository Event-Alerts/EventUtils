package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract Component getName();
    @Shadow public abstract EntityType<?> getType();
    //? if >=1.21.11 {
    /*@Shadow public abstract Vec3 trackingPosition();
    *///?} else {
    @Shadow public abstract Vec3 position();
    //?}

    @Inject(method = "canSpawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void canSpawnSprintParticle(CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().player == null) return;
        final EntityType<?> type = getType();

        // Get position
        //? if >=1.21.11 {
        /*final Vec3 position = trackingPosition();
        *///?} else {
        final Vec3 position = position();
        //?}

        if (((Object) this) instanceof LocalPlayer player) {
            // Players
            if (!EventUtils.MOD.groupManager.isPlayerVisible(player.getGameProfile(), position)) cir.setReturnValue(false);
        } else {
            // Entity
            if (!EventUtils.MOD.groupManager.isEntityVisible(type, position)) cir.setReturnValue(false);
        }
    }
}
