package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Fetches player data from Event Alerts API to determine unlocked plus tags
 */
public class EventAlertsApi {
    @NotNull private final EventUtils mod;
    /**
     * UUIDs we've cached. This is used to represent a UUID cached that has no best tag (null in map).
     */
    @NotNull private final Set<UUID> cached = ConcurrentHashMap.newKeySet();
    /**
     * Cache: Minecraft UUID -> best unlocked tag. Cleared on world unload
     */
    @NotNull private final ConcurrentHashMap<UUID, PlusTag> cache = new ConcurrentHashMap<>();
    /**
     * UUIDs we've already scheduled a fetch for (avoid duplicate requests until cache clear)
     */
    @NotNull private final Set<UUID> fetchScheduled = ConcurrentHashMap.newKeySet();

    public EventAlertsApi(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    /**
     * Fetch unlocked plus tags for a Minecraft UUID. Returns empty set on failure.
     */
    public void populateCachedBestTag(@NotNull Collection<UUID> uuids) {
        final Set<UUID> uuidsToFetch = uuids.stream()
                .filter(uuid -> !cached.contains(uuid) && fetchScheduled.add(uuid))
                .collect(Collectors.toSet());
        if (uuidsToFetch.isEmpty()) return;

        EventUtils.LOGGER.debug("[API] Fetching tags for uuids={}", uuidsToFetch);
        MiscUtility.IO_SCHEDULER.execute(() -> {
            try {
                // Retrieve players
                final List<EAPlayer> players = mod.http.players.retrieveMany(uuidsToFetch.size(), Map.of("minecraft_uuid", uuidsToFetch)).complete();
                if (players == null) {
                    EventUtils.LOGGER.debug("[API] parse: players is null, returning empty set");
                    return;
                }
                EventUtils.LOGGER.debug("[API] parse: fetched players={}", players);

                for (final EAPlayer player : players) {
                    if (player.minecraft == null || player.minecraft.uuid == null) continue;

                    // Get best unlocked tag
                    PlusTag bestTag = null;
                    for (final PlusTag tag : PlusTag.values()) {
                        if (tag.isUnlocked.test(player)) {
                            bestTag = tag;
                            EventUtils.LOGGER.debug("[API] parse: +{} (isUnlocked)", tag);
                            break;
                        }
                    }
                    EventUtils.LOGGER.debug("[API] fetched best tag={} for uuid={}", bestTag, player.minecraft.uuid);

                    // Add to cache
                    cached.add(player.minecraft.uuid);
                    if (bestTag != null) cache.put(player.minecraft.uuid, bestTag);
                    fetchScheduled.remove(player.minecraft.uuid);
                }
            } catch (final Exception e) {
                EventUtils.LOGGER.warn("[API] Fetch failed", e);
            }
        });
    }

    public void populateCachedBestTag(@NotNull UUID uuid) {
        populateCachedBestTag(Set.of(uuid));
    }

    /**
     * Clear cache (e.g. on disconnect)
     */
    public void clearCache() {
        int size = cached.size();
        cached.clear();
        cache.clear();
        fetchScheduled.clear();
        EventUtils.LOGGER.debug("[API] Cache cleared (was {} entries)", size);
    }

    public boolean isCached(@NotNull UUID minecraftUuid) {
        return cached.contains(minecraftUuid);
    }

    /**
     * Get cached best tag for UUID, or null if not cached
     */
    @Nullable
    public PlusTag getCached(@NotNull UUID minecraftUuid) {
        return cache.get(minecraftUuid);
    }
}
