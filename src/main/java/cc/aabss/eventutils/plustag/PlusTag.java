package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;


/**
 * Plus (+) icon tag displayed next to names. Unlocked by linking Discord and roles.
 * Each tag uses its own texture file.
 * <br><b>Order of this enum matters for priority!</b>
 */
public enum PlusTag {
    RED("red", "eventutils.plustag.unlock.admin"),
    BLUE("blue", "eventutils.plustag.unlock.contributor"),
    GOLD("gold", "eventutils.plustag.unlock.bee"),
    PINK("pink", "eventutils.plustag.unlock.booster"),
    WHITE("white", null),
    EMPTY("empty", "eventutils.plustag.unlock.linked");

    @NotNull public final String key;
    @NotNull public final Identifier textureId;
    @NotNull public final String unlockKey;

    PlusTag(@NotNull String key, @Nullable String unlockKey) {
        this.key = key;
        this.textureId = Identifier.of("eventutils", "textures/bee/" + key + ".png");
        this.unlockKey = Objects.requireNonNullElse(unlockKey, "eventutils.plustag.unlock.none");
    }

    /**
     * Pick the best tag to show for another player from their unlocked set
     * */
    @Nullable
    public static PlusTag pickBestForDisplay(@Nullable Set<PlusTag> unlocked) {
        if (unlocked == null || unlocked.isEmpty()) {
            EventUtils.LOGGER.debug("[PlusTag] pickBestForDisplay: unlocked={} -> null", unlocked);
            return null;
        }

        // Get best/highest ranking
        PlusTag best = null;
        for (final PlusTag tag : unlocked) {
            if (tag == WHITE) continue;
            if (best == null || tag.ordinal() < best.ordinal()) best = tag;
        }
        EventUtils.LOGGER.debug("[PlusTag] pickBestForDisplay: unlocked={} -> best={}", unlocked, best);
        return best;
    }
}
