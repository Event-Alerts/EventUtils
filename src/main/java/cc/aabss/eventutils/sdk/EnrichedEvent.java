package cc.aabss.eventutils.sdk;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.http.action.EAAction;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.parents.Stringable;


public class EnrichedEvent {
    @NotNull private final EventUtils mod;
    @NotNull public final EAEvent event;
    @Nullable private EAPlayer host;

    public EnrichedEvent(@NotNull EventUtils mod, @NotNull EAEvent event, @Nullable EAPlayer host) {
        this.mod = mod;
        this.event = event;
        this.host = host;
    }

    public EnrichedEvent(@NotNull EventUtils mod, @NotNull EAEvent event) {
        this(mod, event, null);
    }

    @NotNull
    public EAAction<EAPlayer> getHost() {
        if (host != null) return EAAction.completed(host);
        if (event.host == null) return EAAction.completed(null);
        return mod.http.players.retrieveOneByDiscordId(event.host)
                .onErrorReturnNull()
                .onSuccess(host -> this.host = host);
    }

    @Override @NotNull
    public String toString() {
        return Stringable.toString(this, "mod");
    }
}
