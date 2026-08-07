package cc.aabss.eventutils.cache;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.http.action.EAAction;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public abstract class Cache<K, V> {
    @NotNull protected final ConcurrentMap<K, Optional<V>> cache = new ConcurrentHashMap<>();
    @NotNull protected final ConcurrentMap<K, EAAction<V>> inFlight = new ConcurrentHashMap<>();

    @NotNull @CheckReturnValue
    public EAAction<V> get(@NotNull K uuid) {
        // Is cached
        final Optional<V> cached = cache.get(uuid);
        EventUtils.LOGGER.trace("[CACHE] hit for key {}: {}", uuid, cached);
        if (cached != null) return EAAction.completed(cached.orElse(null));

        // Not cached yet, fetch
        EventUtils.LOGGER.trace("[CACHE] miss for key {}", uuid);
        return fetch(uuid);
    }

    @NotNull @CheckReturnValue
    public EAAction<Set<V>> get(@NotNull Collection<K> uuids) {
        // Get cached vs missing values
        final Set<V> cached = new HashSet<>();
        final Set<K> missing = new HashSet<>();
        for (final K uuid : uuids) {
            final Optional<V> player = cache.get(uuid);
            if (player != null) {
                player.ifPresent(cached::add);
            } else {
                missing.add(uuid);
            }
        }
        EventUtils.LOGGER.trace("[CACHE] hits: {}", cached);

        // Fetch missing players
        if (missing.isEmpty()) return EAAction.completed(cached);
        EventUtils.LOGGER.trace("[CACHE] misses: {}", missing);
        return fetch(missing).map(fetched -> {
            // Merge cached and fetched players
            final Set<V> all = new HashSet<>(cached);
            all.addAll(fetched);
            return all;
        });
    }

    public void addToCache(@NotNull K key, @Nullable V value) {
        cache.put(key, Optional.ofNullable(value));
    }

    protected void removeFromCache(@NotNull K key) {
        cache.remove(key);
    }

    protected void removeFromCache(@NotNull Collection<K> keys) {
        keys.forEach(this::removeFromCache);
    }

    public void clear() {
        cache.clear();
        inFlight.clear();
    }

    @NotNull @CheckReturnValue
    private EAAction<V> fetch(@NotNull K key) {
        return inFlight.computeIfAbsent(key, keyFlight -> fetchImpl(keyFlight)
                .map(fetched -> {
                    EventUtils.LOGGER.trace("[CACHE] fetched with key {}", keyFlight);
                    addToCache(keyFlight, fetched);
                    return fetched;
                })
                .onError(t -> {
                    removeFromCache(keyFlight);
                    EventUtils.LOGGER.warn("Failed to fetch with key {}, removed from cache", keyFlight, t);
                })
                .onComplete(() -> inFlight.remove(keyFlight)));
    }

    @NotNull @CheckReturnValue
    private EAAction<Set<V>> fetch(@NotNull Collection<K> keys) {
        if (keys.isEmpty()) return EAAction.completed(Collections.emptySet());
        return fetchImpl(keys)
                .map(fetched -> {
                    // Cache found values
                    final Set<K> foundKeys = new HashSet<>();
                    final Set<V> values = new HashSet<>();
                    for (final Map.Entry<K, V> entry : fetched.entrySet()) {
                        final K key = entry.getKey();
                        final V value = entry.getValue();
                        addToCache(key, value);
                        foundKeys.add(key);
                        values.add(value);
                    }
                    EventUtils.LOGGER.trace("[CACHE] fetched with keys {}", foundKeys);

                    // Cache missing values as empty
                    for (final K key : keys) if (!foundKeys.contains(key)) addToCache(key, null);

                    return values;
                })
                .onError(t -> removeFromCache(keys));
    }

    @NotNull @CheckReturnValue
    protected abstract EAAction<V> fetchImpl(@NotNull K key);

    @NotNull @CheckReturnValue
    protected abstract EAAction<Map<K, V>> fetchImpl(@NotNull Collection<K> keys);
}
