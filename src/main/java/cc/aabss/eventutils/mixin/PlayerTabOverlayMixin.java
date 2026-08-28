package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.plustag.IconRenderer;
import cc.aabss.eventutils.plustag.PlusTag;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.stream.Collectors;


@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Shadow @Final private Minecraft minecraft;

    // We want to cache on both join AND tab open just in-case any players were missed on-join
    @Inject(method = "render", at = @At("HEAD"))
    private void eventutils$populatePlayersCache(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        if (minecraft.player == null) return;
        final ClientPacketListener packetListener = minecraft.getConnection();
        if (packetListener == null) return;
        EventUtils.MOD.cacheManager.players()
                .get(packetListener.getListedOnlinePlayers().stream()
                        .map(entry -> new VersionedGameProfile(entry.getProfile()).getId())
                        .collect(Collectors.toSet()))
                .queue();
    }

    @Inject(method = "renderLatencyIcon", at = @At("TAIL"), require = 0)
    private void eventutils$drawPlusTagNextToName(GuiGraphics guiGraphics, int width, int x, int y, PlayerInfo entry, CallbackInfo ci) {
        drawPlusTagNextToName(guiGraphics, x, y, entry);
    }

    // Lunar is super annoying and breaks "renderLatencyIcon" (never called). So we inject into their custom handler for it.
    // We can't add Lunar to classpath so @Inject will show errors. This is expected and okay.
    @Inject(method = "handler$bbk000$lunar$drawPing$v1_20_0", at = @At("TAIL"), require = 0)
    private void eventutils$drawPlusTagNextToNameLunar(GuiGraphics guiGraphics, int width, int x, int y, PlayerInfo entry, CallbackInfo lunarCi, CallbackInfo ci) {
        drawPlusTagNextToName(guiGraphics, x, y, entry);
    }

    @Unique
    private void drawPlusTagNextToName(GuiGraphics guiGraphics, int x, int y, PlayerInfo entry) {
        if (minecraft.player == null) return;

        // Bee icons disabled
        if (!EventUtils.MOD.config.bee_icons) return;

        // Get UUID
        final UUID uuid = new VersionedGameProfile(entry.getProfile()).getId();

        // Self
        if (minecraft.player.getUUID().equals(uuid)) {
            if (EventUtils.MOD.authManager.player != null) drawIcon(guiGraphics, x, y, EventUtils.MOD.authManager.player.getPlusTag(), true);
            return;
        }

        EventUtils.MOD.cacheManager.players().get(uuid).queue(cached -> {
            if (cached != null) drawIcon(guiGraphics, x, y, cached.getPlusTag(), cached.isOnline());
        });
    }

    @Unique
    private static void drawIcon(GuiGraphics guiGraphics, int x, int y, @Nullable PlusTag tag, boolean online) {
        final int iconSize = 10;
        final int iconX = x - 10;
        IconRenderer.draw(guiGraphics, online ? PlusTag.BEE_GREEN : PlusTag.BEE, iconX, y, iconSize);
        if (tag != null) IconRenderer.draw(guiGraphics, tag.textureId, iconX, y, iconSize);
    }
}
