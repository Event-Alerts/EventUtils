package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.util.Identifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Plus (+) icon tag displayed next to names. Unlocked by linking Discord and roles.
 * Each tag uses its own texture file (no in-game slicing).
 */
public enum PlusTag {
    /** Linked (gray) - from linking Discord */
    NONE("none", "linked", "eventutils.plustag.unlock.linked"),
    /** Unused (white +) */
    WHITE("white", "white", null),
    /** Admin */
    RED("red", "admin", "eventutils.plustag.unlock.admin"),
    /** Developer / Contributor */
    BLUE("blue", "contrib", "eventutils.plustag.unlock.contributor"),
    /** Bee subscription */
    ORANGE("orange", "bee", "eventutils.plustag.unlock.bee"),
    /** Booster */
    PINK("pink", "booster", "eventutils.plustag.unlock.booster");

    private final String key;
    @NotNull private final Identifier textureId;
    @NotNull private final String unlockKey;

    PlusTag(@NotNull String key, @NotNull String textureName, String unlockKey) {
        this.key = key;
        this.textureId = Identifier.of("eventutils", "textures/gui/" + textureName + ".png");
        this.unlockKey = unlockKey != null ? unlockKey : "eventutils.plustag.unlock.none";
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public Identifier getTextureId() {
        return textureId;
    }

    @NotNull
    public String getUnlockKey() {
        return unlockKey;
    }

    /** Priority for "best" tag when we don't know the player's choice (higher = show first). */
    private int displayPriority() {
        return switch (this) {
            case RED -> 5;
            case BLUE -> 4;
            case ORANGE -> 3;
            case PINK -> 2;
            case NONE -> 1;
            case WHITE -> 0;
        };
    }

    /** Pick the best tag to show for another player from their unlocked set (admin > contrib > bee > booster > linked). */
    @Nullable
    public static PlusTag pickBestForDisplay(@Nullable Set<PlusTag> unlocked) {
        if (unlocked == null || unlocked.isEmpty()) {
            EventUtils.LOGGER.debug("[PlusTag] pickBestForDisplay: unlocked={} -> null", unlocked);
            return null;
        }
        PlusTag best = null;
        for (PlusTag tag : unlocked) {
            if (tag == WHITE) continue;
            if (best == null || tag.displayPriority() > best.displayPriority()) best = tag;
        }
        EventUtils.LOGGER.debug("[PlusTag] pickBestForDisplay: unlocked={} -> best={}", unlocked, best);
        return best;
    }
}
