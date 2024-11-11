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
        ClientPlayerEntity mainPlayer = MinecraftClient.getInstance().player;
        assert mainPlayer != null;

        // Non-players (mob)
        if (!(this.getType() == EntityType.PLAYER)) {
            if (EventUtils.MOD.config.hiddenEntityTypes.contains(this.getType())) {
                ci.cancel();
            } else {
                if (mainPlayer.getPos().distanceTo(this.getPos()) <= EventUtils.MOD.config.hidePlayersRadius) {
                    ci.cancel();
                }
            }
            return;
        }

        // Players
        final String name = this.getName().getString().toLowerCase();
        if (!EventUtils.MOD.config.whitelistedPlayers.contains(name) // Check if player whitelisted
                && !name.contains("[") && !name.contains("]") && !name.contains(" ") && !name.contains("-")) { // Check if player is an NPC
            if (EventUtils.MOD.config.hidePlayersRadius == 1) {
                ci.cancel();
            } else {
                if (mainPlayer.getPos().distanceTo(this.getPos()) <= EventUtils.MOD.config.hidePlayersRadius) {
                    ci.cancel();
                }
            }
        }
    }
}
