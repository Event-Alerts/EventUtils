package cc.aabss.eventutils.cache;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EnrichedPlayer;
import gg.eventalerts.sdk.http.action.EAAction;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PlayerCache extends Cache<UUID, EnrichedPlayer> {
    @NotNull private final EventUtils mod;

    public PlayerCache(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void addToCache(@NotNull Collection<EAPlayer> players) {
        players.forEach(player -> {
            final EnrichedPlayer enriched = new EnrichedPlayer(player);
            if (enriched.player.minecraft != null && enriched.player.minecraft.uuid != null) {
                addToCache(enriched.player.minecraft.uuid, enriched);
            }
        });
    }

    @Override @NotNull
    protected EAAction<EnrichedPlayer> fetchImpl(@NotNull UUID uuid) {
        return mod.http.players.retrieveOneByMinecraftUuid(uuid).map(EnrichedPlayer::new);
    }

    @Override @NotNull
    protected EAAction<Map<UUID, EnrichedPlayer>> fetchImpl(@NotNull Collection<UUID> uuids) {
        if (uuids.isEmpty()) return EAAction.completed(Collections.emptyMap());
        return mod.http.players.retrieveMany(uuids.size(), null, Map.of("minecraft_uuid", uuids))
                .map(players -> {
                    final Map<UUID, EnrichedPlayer> result = new HashMap<>();
                    for (final EAPlayer player : players) {
                        final EnrichedPlayer enriched = new EnrichedPlayer(player);
                        if (enriched.player.minecraft != null && enriched.player.minecraft.uuid != null) {
                            result.put(enriched.player.minecraft.uuid, enriched);
                        }
                    }
                    return result;
                });
    }
}
