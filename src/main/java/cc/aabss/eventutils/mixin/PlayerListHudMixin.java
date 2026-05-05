package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.plustag.EventAlertsApi;
import cc.aabss.eventutils.plustag.PlusTag;
import cc.aabss.eventutils.plustag.PlusTagRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;

import java.util.EnumSet;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow @Final private MinecraftClient client;

    /** Draw + icon for every tab list row (local player = selected tag, others = best unlocked from Event Alerts). */
    @Inject(method = "renderLatencyIcon", at = @At("TAIL"))
    private void eventutils$drawPlusTagNextToName(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (client.player == null) return;

        //? if >=1.21.11 {
        /*String name = entry.getProfile().name();
        final boolean isLocal = entry.getProfile().id().equals(client.player.getUuid());
        UUID uuid = entry.getProfile().id();
        *///?} else {
        String name = entry.getProfile().getName();
        final boolean isLocal = entry.getProfile().getId().equals(client.player.getUuid());
        UUID uuid = entry.getProfile().getId();
        //?}

        if (isLocal && EventAlertsApi.getCached(uuid) == null) {
            // If JOIN ran before player was ready, trigger it when tab list is drawn
            EventUtils.LOGGER.info("[EventUtils] Tab list: local player not cached, scheduling Event Alerts fetch uuid={}", uuid);
            EventAlertsApi.scheduleFetchIfNeeded(uuid);
        }

        EnumSet<PlusTag> cached = EventAlertsApi.getCached(uuid);
        if (cached == null) {
            EventUtils.LOGGER.debug("[TabList] entry={} uuid={} cache MISS, scheduling fetch", name, uuid);
            EventAlertsApi.scheduleFetchIfNeeded(uuid);
            return;
        }

        final PlusTag tag = PlusTag.pickBestForDisplay(cached);
        EventUtils.LOGGER.debug("[TabList] entry={} uuid={} cache HIT unlocked={} pickBest={}", name, uuid, cached, tag);

        if (tag == null || tag == PlusTag.WHITE) {
            EventUtils.LOGGER.debug("[TabList] entry={} skip draw: tag={} (null or WHITE)", name, tag);
            return;
        }

        int iconSize = 10;
        int iconX = x - 10;
        PlusTagRenderer.draw(context, tag, iconX, y, iconSize);
        EventUtils.LOGGER.debug("[TabList] entry={} DRAW tag={} at ({}, {}) size={}", name, tag, iconX, y, iconSize);
    }
}
