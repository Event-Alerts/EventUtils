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

    /**
     * Fetch unlocked plus tags for a Minecraft UUID. Returns empty set on failure.
     */
    public void populateCachedUnlockedTags(@NotNull UUID minecraftUuid) {
        // Check if already cached
        final EnumSet<PlusTag> cached = cache.get(minecraftUuid);
        if (cached != null) {
            EventUtils.LOGGER.debug("[API] fetchUnlockedTags: cache HIT uuid={} tags={}", minecraftUuid, cached);
            return;
        }

        EventUtils.LOGGER.debug("[API] Fetching tags for uuid={}", minecraftUuid);
        try {
            // Retrieve EAPlayer
            final EAPlayer player = mod.http.players.retrieveOneByMinecraftUuid(minecraftUuid).complete();
            if (player == null) {
                EventUtils.LOGGER.debug("[API] parse: player is null, returning empty set");
                return;
            }

            // Get unlocked tags
            final EnumSet<PlusTag> unlocked = EnumSet.noneOf(PlusTag.class);
            for (final PlusTag tag : PlusTag.values()) {
                if (tag.isUnlocked.test(player)) {
                    unlocked.add(tag);
                    EventUtils.LOGGER.debug("[API] parse: +{} (isUnlocked)", tag);
                }
            }

            // Add to cache
            EventUtils.LOGGER.debug("[API] fetched unlocked tags={} for uuid={}", unlocked, minecraftUuid);
            cache.put(minecraftUuid, unlocked);
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("[API] Fetch failed uuid={} error={}", minecraftUuid, e.getMessage(), e);
        }
    }

    /**
     * Clear cache (e.g. on disconnect)
     */
    public void clearCache() {
        int size = cache.size();
        cache.clear();
        fetchScheduled.clear();
        EventUtils.LOGGER.debug("[API] Cache cleared (was {} entries)", size);
    }

    /**
     * Schedule a fetch for this UUID if not cached and not already scheduled. Call from main thread
     */
    public void scheduleFetchIfNeeded(@NotNull UUID minecraftUuid) {
        if (cache.containsKey(minecraftUuid)) return;
        if (!fetchScheduled.add(minecraftUuid)) return; // already scheduled
        EventUtils.LOGGER.debug("[API] Scheduling fetch for uuid={}", minecraftUuid);
        EventUtils.MOD.scheduler.execute(() -> populateCachedUnlockedTags(minecraftUuid));
    }

    /**
     * Get cached unlocked tags for UUID, or null if not cached. (No per-call debug log to avoid spam from tab list.)
     */
    @Nullable
    public EnumSet<PlusTag> getCached(@NotNull UUID minecraftUuid) {
        return cache.get(minecraftUuid);
    }
}
