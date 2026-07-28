package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.plustag.IconRenderer;
import cc.aabss.eventutils.plustag.PlusTag;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
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
        EventUtils.MOD.cacheManager.players().get(networkHandler.getListedPlayerListEntries().stream()
                .map(entry -> new VersionedGameProfile(entry.getProfile()).getId())
                .collect(Collectors.toSet()));
    }

    /**
     * Draw plus icon for every tab list row
     */
    @Inject(method = "renderLatencyIcon", at = @At("TAIL"))
    private void eventutils$drawPlusTagNextToName(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (client.player == null) return;

        // Bee icons disabled
        if (!EventUtils.MOD.config.beeIcons) return;

        // Get UUID
        final UUID uuid = new VersionedGameProfile(entry.getProfile()).getId();

        // Self
        if (client.player.getUuid().equals(uuid)) {
            if (EventUtils.MOD.authManager.player != null) drawIcon(context, x, y, EventUtils.MOD.authManager.player.getPlusTag(), true);
            return;
        }

        EventUtils.MOD.cacheManager.players().get(uuid).queue(cached -> {
            if (cached != null) drawIcon(context, x, y, cached.getPlusTag(), cached.isOnline());
        });
    }

    @Unique
    private static void drawIcon(DrawContext context, int x, int y, @Nullable PlusTag tag, boolean online) {
        final int iconSize = 10;
        final int iconX = x - 10;
        IconRenderer.draw(context, online ? PlusTag.BEE_GREEN : PlusTag.BEE, iconX, y, iconSize);
        if (tag != null) IconRenderer.draw(context, tag.textureId, iconX, y, iconSize);
    }
}
