package cc.aabss.eventutils.cache;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.http.action.EAAction;
import gg.eventalerts.sdk.http.exception.EAHttpResponseException;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public abstract class Cache<K, V> {
    @NotNull protected final ConcurrentMap<K, Optional<V>> cache = new ConcurrentHashMap<>();
    @NotNull protected final ConcurrentMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

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
        final CompletableFuture<V> future = inFlight.computeIfAbsent(key, keyFlight -> fetchImpl(keyFlight)
                .onSuccess(fetched -> {
                    EventUtils.LOGGER.trace("[CACHE] fetched with key {}", keyFlight);
                    addToCache(keyFlight, fetched);
                })
                .onError(t -> {
                    removeFromCache(keyFlight);

                    // Don't log rate limits
                    if (t instanceof EAHttpResponseException e && e.getStatusCode() == 429) return;
                    EventUtils.LOGGER.warn("Failed to fetch with key {}, removed from cache", keyFlight, t);
                })
                .onComplete(() -> inFlight.remove(keyFlight))
                .submit());
        return new EAAction<>("cached-fetch:" + key, future::join);
    }

    @NotNull @CheckReturnValue
    private EAAction<Set<V>> fetch(@NotNull Collection<K> keys) {
        if (keys.isEmpty()) return EAAction.completed(Collections.emptySet());

        // Only fetch keys that aren't already being fetched (individually or by another batch)
        final Set<K> newKeys = new HashSet<>();
        for (final K key : keys) if (!inFlight.containsKey(key)) newKeys.add(key);

        if (!newKeys.isEmpty()) {
            final CompletableFuture<Map<K, V>> batch = fetchImpl(newKeys)
                    .onSuccess(fetched -> {
                        // Cache found values
                        final Set<K> foundKeys = new HashSet<>();
                        for (final Map.Entry<K, V> entry : fetched.entrySet()) {
                            final K key = entry.getKey();
                            addToCache(key, entry.getValue());
                            foundKeys.add(key);
                        }
                        EventUtils.LOGGER.trace("[CACHE] fetched with keys {}", foundKeys);

                        // Cache missing values as empty
                        for (final K key : newKeys) if (!foundKeys.contains(key)) addToCache(key, null);
                    })
                    .onError(t -> removeFromCache(newKeys))
                    .onComplete(() -> newKeys.forEach(inFlight::remove))
                    .submit();

            // Register each new key so concurrent individual/bulk calls dedupe against this batch
            for (final K key : newKeys) inFlight.put(key, batch.thenApply(fetched -> fetched.get(key)));
        }

        // Wait for all requested keys (new + already in-flight) to settle, then read from cache
        final List<CompletableFuture<?>> waits = new ArrayList<>();
        for (final K key : keys) {
            final CompletableFuture<V> future = inFlight.get(key);
            if (future != null) waits.add(future);
        }
        final CompletableFuture<Void> all = CompletableFuture.allOf(waits.toArray(new CompletableFuture[0]));

        return new EAAction<>("cached-fetch-batch", () -> {
            all.join();
            final Set<V> values = new HashSet<>();
            for (final K key : keys) {
                final Optional<V> cached = cache.get(key);
                if (cached != null) cached.ifPresent(values::add);
            }
            return values;
        });
    }

    @NotNull @CheckReturnValue
    protected abstract EAAction<V> fetchImpl(@NotNull K key);

    @NotNull @CheckReturnValue
    protected abstract EAAction<Map<K, V>> fetchImpl(@NotNull Collection<K> keys);
}
