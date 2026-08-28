package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.versioning.VersionedIdentifier;
import gg.eventalerts.sdk.object.EAPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;


/**
 * Plus (+) icon tag displayed next to names. Unlocked by linking Discord and roles.
 * Each tag uses its own texture file.
 * <br><b>Order of this enum matters for priority!</b>
 */
public enum PlusTag {
    RED(player -> player.discord != null && player.discord.roles != null && player.discord.roles.contains(EAPlayer.Discord.Role.ADMIN)),

    LIGHT_RED(player -> player.discord != null && player.discord.roles != null && player.discord.roles.contains(EAPlayer.Discord.Role.STAFF)),

    BLUE(player -> player.discord != null && player.discord.roles != null && player.discord.roles.contains(EAPlayer.Discord.Role.CONTRIBUTOR)),

    AQUA(player -> player.subscription != null && player.subscription.tier == EAPlayer.Subscription.Tier.HORNET),

    GREEN(player -> player.subscription != null && player.subscription.tier == EAPlayer.Subscription.Tier.WASP),

    GOLD(player -> player.subscription != null && player.subscription.tier == EAPlayer.Subscription.Tier.BEE),

    WHITE(player -> player.discord != null && player.minecraft != null);

    @NotNull public static final Object BEE = VersionedIdentifier.of("textures/bee/bee.png");
    @NotNull public static final Object BEE_GREEN = VersionedIdentifier.of("textures/bee/bee_green.png");

    @NotNull public final Object textureId;
    @NotNull public final Predicate<EAPlayer> isUnlocked;

    PlusTag(@NotNull Predicate<EAPlayer> isUnlocked) {
        this.textureId = VersionedIdentifier.of("textures/bee/plus/" + name().toLowerCase() + ".png");
        this.isUnlocked = isUnlocked;
    }

    @Nullable
    public static PlusTag getBestUnlocked(@Nullable EAPlayer player) {
        if (player == null) return null;
        PlusTag bestTag = null;
        for (final PlusTag tag : PlusTag.values()) {
            if (tag.isUnlocked.test(player)) {
                bestTag = tag;
                break;
            }
        }
        EventUtils.LOGGER.debug("[API] Fetched best tag={} uuid={}", bestTag, player.minecraft != null ? player.minecraft.uuid : "(player.minecraft=null)");
        return bestTag;
    }
}
