package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.plustag.PlusTag;
import cc.aabss.eventutils.plustag.PlusTagRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.stream.Collectors;


@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow @Final private MinecraftClient client;

    /**
     * Fetch and cache all players' tags
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void eventutils$populateMissingTags(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (client.player == null) return;
        final ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) return;
        EventUtils.MOD.api.populateCachedBestTag(networkHandler.getListedPlayerListEntries().stream()
                .map(entry -> {
                    //? if >=1.21.11 {
                    /*return entry.getProfile().id();
                    *///?} else {
                    return entry.getProfile().getId();
                    //?}
                })
                .collect(Collectors.toSet()));
    }

    /**
     * Draw plus icon for every tab list row
     */
    @Inject(method = "renderLatencyIcon", at = @At("TAIL"))
    private void eventutils$drawPlusTagNextToName(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (client.player == null) return;

        // Get UUID and name
        //? if >=1.21.11 {
        /*final UUID uuid = entry.getProfile().id();
        final String name = entry.getProfile().name();
        *///?} else {
        final UUID uuid = entry.getProfile().getId();
        final String name = entry.getProfile().getName();
        //?}

        // Fetch if not cached
        if (!EventUtils.MOD.api.isCached(uuid)) {
            EventUtils.MOD.api.populateCachedBestTag(uuid);
            return;
        }

        // Get best tag from cache
        final PlusTag cached = EventUtils.MOD.api.getCached(uuid);
        if (cached == null) return; // no tags

        // Draw tag
        final int iconSize = 10;
        final int iconX = x - 10;
        PlusTagRenderer.draw(context, cached, iconX, y, iconSize);
    }
}
