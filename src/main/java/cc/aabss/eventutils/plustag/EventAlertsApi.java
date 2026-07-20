package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Fetches player data from Event Alerts API to determine unlocked plus tags
 */
public class EventAlertsApi {
    @NotNull private static final Duration FAILED_ATTEMPT_DELAY = Duration.ofSeconds(30);

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
    @Nullable private Long lastFailedAttempt;

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

        // Check lastFailedAttempt
        if (lastFailedAttempt != null && System.currentTimeMillis() - lastFailedAttempt < FAILED_ATTEMPT_DELAY.toMillis()) return;

        EventUtils.LOGGER.debug("[API] Fetching tags for uuids={}", uuidsToFetch);
        try {
            // Retrieve players
            mod.http.players.retrieveMany(uuidsToFetch.size(), Map.of("minecraft_uuid", uuidsToFetch)).queue(players -> {
                if (players == null) {
                    EventUtils.LOGGER.debug("[API] parse: players is null, returning empty set");
                    fetchScheduled.removeAll(uuidsToFetch);
                    return;
                }
                EventUtils.LOGGER.debug("[API] parse: fetched players={}", players);

                for (final EAPlayer player : players) {
                    if (player.minecraft == null || player.minecraft.uuid == null) {
                        EventUtils.LOGGER.debug("[API] parse: player.minecraft.uuid is null, skipping");
                        fetchScheduled.remove(player.minecraft.uuid);
                        continue;
                    }

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
            }, t -> {
                EventUtils.LOGGER.warn("[API] Fetch failed", t);
                fetchScheduled.removeAll(uuidsToFetch);
                lastFailedAttempt = System.currentTimeMillis();
            });
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("[API] Fetch failed", e);
            fetchScheduled.removeAll(uuidsToFetch);
            lastFailedAttempt = System.currentTimeMillis();
        }
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
