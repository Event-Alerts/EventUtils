package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.plustag.IconRenderer;
import cc.aabss.eventutils.plustag.PlusTag;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;


@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow @Final private MinecraftClient client;

    // Set true by the per-entry hook below. Used to detect clients (Lunar, Feather, ...) that
    // replace the vanilla tab list rendering and never hit our injection point.
    @Unique private static boolean eventutils$iconHookFired = false;
    @Unique private static int eventutils$rendersWithoutHook = 0;
    @Unique private static boolean eventutils$loggedIncompatibleClient = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void eventutils$populatePlayersCache(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (client.player == null) return;
        final ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) return;
        final Collection<PlayerListEntry> entries = networkHandler.getListedPlayerListEntries();

        // We want to cache on both join AND tab open just in-case any players were missed on-join
        EventUtils.MOD.cacheManager.players()
                .get(entries.stream()
                        .map(entry -> new VersionedGameProfile(entry.getProfile()).getId())
                        .collect(Collectors.toSet()))
                .queue();

        // If bee icons are on and there are players to draw them on, but our per-entry hook hasn't
        // fired for a few frames, the client has replaced vanilla tab rendering. Warn once rather
        // than failing silently. (This HEAD sees the previous frame's hook result.)
        if (EventUtils.MOD.config.bee_icons && !eventutils$loggedIncompatibleClient && !entries.isEmpty()) {
            if (eventutils$iconHookFired) {
                eventutils$rendersWithoutHook = 0;
            } else if (++eventutils$rendersWithoutHook >= 3) {
                eventutils$loggedIncompatibleClient = true;
                EventUtils.LOGGER.warn("[BEE ICONS] Vanilla PlayerListHud rendering not found - an incompatible client (Lunar Client, Feather, ...) has likely replaced it, bee icons will not show!");
            }
        }
        eventutils$iconHookFired = false;
    }

    // Anchor to the player-name text draw. Unlike the ping icon (rewritten by Lunar) and the head
    // blit (only drawn when tab heads are enabled), this call runs once per entry on every version
    // and every client that shows a vanilla-style tab list.
    // arg 2/3 are the name's x/y; the head, when present, occupies the 8px slot immediately to its left.
    @ModifyArgs(
        method = "render",
        at = @At(value = "INVOKE",
            //? if >=1.21.6 {
            /*target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"
            *///?} else {
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"
            //?}
        ),
        require = 0)
    private void eventutils$drawBeeIcon(Args args, @Local(argsOnly = true) DrawContext context, @Local PlayerListEntry entry) {
        eventutils$iconHookFired = true;
        drawPlusTagNextToName(context, args.get(2), args.get(3), entry);
    }

    @Unique
    private void drawPlusTagNextToName(DrawContext context, int x, int y, PlayerListEntry entry) {
        if (client.player == null) return;

        // Bee icons disabled
        if (!EventUtils.MOD.config.bee_icons) return;

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
        final int iconX = x - 10 - iconSize;
        IconRenderer.draw(context, online ? PlusTag.BEE_GREEN : PlusTag.BEE, iconX, y, iconSize);
        if (tag != null) IconRenderer.draw(context, tag.textureId, iconX, y, iconSize);
    }
}
