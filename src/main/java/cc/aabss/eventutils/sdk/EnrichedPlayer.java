package cc.aabss.eventutils.sdk;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.plustag.PlusTag;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class EnrichedPlayer extends EAPlayer {
    // --- Enrichment ---
    private boolean plusTagResolved = false;
    @Nullable private PlusTag plusTag;

    public EnrichedPlayer(@NotNull EAPlayer player) {
        super(player);
    }

    public boolean isOnline() {
        return minecraft != null && minecraft.eventUtils != null;
    }

    public boolean isDiscordLinked() {
        return discord != null;
    }

    @Nullable
    public EAPlayer.Subscription.Tier getEffectiveSubscriptionTier() {
        // Admins get highest subscription
        if (discord != null && discord.roles != null && discord.roles.contains(EAPlayer.Discord.Role.ADMIN)) {
            return EAPlayer.Subscription.Tier.HORNET;
        }

        // Has subscription
        if (subscription != null) return subscription.tier;

        // No subscription
        return null;
    }

    @Nullable
    public PlusTag getPlusTag() {
        if (plusTagResolved) return plusTag;

        plusTagResolved = true;
        PlusTag bestTag = null;
        for (final PlusTag tag : PlusTag.values()) {
            if (tag.isUnlocked.test(this)) {
                bestTag = tag;
                break;
            }
        }
        EventUtils.LOGGER.debug("[API] Fetched best tag={} uuid={}", bestTag, minecraft != null ? minecraft.uuid : "(minecraft=null)");
        plusTag = bestTag;
        return bestTag;
    }
}
