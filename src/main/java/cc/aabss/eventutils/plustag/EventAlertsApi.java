package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Fetches player data from Event Alerts API to determine unlocked plus tags
 */
public class EventAlertsApi {
    @NotNull private final EventUtils mod;
    /**
     * Cache: Minecraft UUID -> unlocked tags. Cleared on world unload
     */
    private final ConcurrentHashMap<UUID, EnumSet<PlusTag>> cache = new ConcurrentHashMap<>();
    /**
     * UUIDs we've already scheduled a fetch for (avoid duplicate requests until cache clear)
     */
    private final Set<UUID> fetchScheduled = ConcurrentHashMap.newKeySet();

    public EventAlertsApi(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    /** Fetch unlocked plus tags for a Minecraft UUID. Returns empty set on failure. */
    @NotNull
    public EnumSet<PlusTag> fetchUnlockedTags(@NotNull UUID minecraftUuid) {
        final EnumSet<PlusTag> cached = cache.get(minecraftUuid);
        if (cached != null) {
            EventUtils.LOGGER.debug("[API] fetchUnlockedTags: cache HIT uuid={} tags={}", minecraftUuid, cached);
            return cached;
        }

        EventUtils.LOGGER.info("[API] Fetching tags for uuid={}", minecraftUuid);
        try {
            final EAPlayer player = mod.http.players.retrieveOneByMinecraftUuid(minecraftUuid).complete();
            final EnumSet<PlusTag> unlocked = parseUnlockedTags(player);
            EventUtils.LOGGER.debug("[API] parsed unlocked tags={} for uuid={}", unlocked, minecraftUuid);
            cache.put(minecraftUuid, unlocked);
            EventUtils.LOGGER.info("[API] Fetched uuid={} tags={}", minecraftUuid, unlocked);
            return unlocked;
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("[API] Fetch failed uuid={} error={}", minecraftUuid, e.getMessage(), e);
            return EnumSet.noneOf(PlusTag.class);
        }
    }

    /**
     * Parse API response into unlocked tags
     */
    @NotNull
    private static EnumSet<PlusTag> parseUnlockedTags(@Nullable EAPlayer player) {
        final EnumSet<PlusTag> tags = EnumSet.noneOf(PlusTag.class);
        if (player == null || player.discord == null) {
            EventUtils.LOGGER.debug("[API] parse: player or player.discord is null, returning empty set");
            return tags;
        }

        // Linked: has minecraft
        if (player.minecraft != null) {
            tags.add(PlusTag.EMPTY); // "Linked" icon
            EventUtils.LOGGER.debug("[API] parse: +EMPTY (linked, discord+minecraft.uuid)");
        }

        // Bee / Premium: subscription tier
        if (player.subscription != null) {
            tags.add(PlusTag.GOLD);
            EventUtils.LOGGER.debug("[API] parse: +GOLD (subscription.tier)");
        }

        if (player.discord.roles != null) {
            // Admin
            if (player.discord.roles.contains(EAPlayer.Discord.Role.ADMIN)) {
                tags.add(PlusTag.RED);
                EventUtils.LOGGER.debug("[API] parse: +RED (roles contains ADMIN)");
            }

            // Contributor
            if (player.discord.roles.contains(EAPlayer.Discord.Role.CONTRIBUTOR)) {
                tags.add(PlusTag.BLUE);
                EventUtils.LOGGER.debug("[API] parse: +BLUE (roles contains DEV/CONTRIBUTOR)");
            }
        }

        return tags;
    }

    /**
     * Clear cache (e.g. on disconnect)
     */
    public void clearCache() {
        int size = cache.size();
        cache.clear();
        fetchScheduled.clear();
        EventUtils.LOGGER.info("[API] Cache cleared (was {} entries)", size);
    }

    /**
     * Schedule a fetch for this UUID if not cached and not already scheduled. Call from main thread
     */
    public void scheduleFetchIfNeeded(@NotNull UUID minecraftUuid) {
        if (cache.containsKey(minecraftUuid)) return;
        if (!fetchScheduled.add(minecraftUuid)) return; // already scheduled
        EventUtils.LOGGER.info("[API] Scheduling fetch for uuid={}", minecraftUuid);
        EventUtils.MOD.scheduler.execute(() -> fetchUnlockedTags(minecraftUuid));
    }

    /**
     * Get cached unlocked tags for UUID, or null if not cached. (No per-call debug log to avoid spam from tab list.)
     */
    @Nullable
    public EnumSet<PlusTag> getCached(@NotNull UUID minecraftUuid) {
        return cache.get(minecraftUuid);
    }
}
