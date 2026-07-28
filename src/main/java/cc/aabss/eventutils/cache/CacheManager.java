package cc.aabss.eventutils.cache;

import cc.aabss.eventutils.EventUtils;
import org.jetbrains.annotations.NotNull;


public record CacheManager(@NotNull PlayerCache players) {
    public CacheManager(@NotNull EventUtils mod) {
        this(new PlayerCache(mod));
    }

    public void clearAll() {
        players.clear();
    }
}
