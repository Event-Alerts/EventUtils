package cc.aabss.eventutils.sdk;

import cc.aabss.eventutils.plustag.PlusTag;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.parents.Stringable;


public class EnrichedPlayer extends Stringable {
    @NotNull public final EAPlayer player;

    // --- Enrichment ---
    private boolean plusTagResolved = false;
    @Nullable private PlusTag plusTag;

    public EnrichedPlayer(@NotNull EAPlayer player) {
        this.player = player;
    }

    public boolean isOnline() {
        return player.minecraft != null && player.minecraft.eventUtils != null;
    }

    @Nullable
    public PlusTag getPlusTag() {
        if (!plusTagResolved) {
            plusTagResolved = true;
            plusTag = PlusTag.getBestUnlocked(player);
        }
        return plusTag;
    }
}
