package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.BuildProperties;
import gg.eventalerts.sdk.object.EAPlayer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;


/**
 * Plus (+) icon tag displayed next to names. Unlocked by linking Discord and roles.
 * Each tag uses its own texture file.
 * <br><b>Order of this enum matters for priority!</b>
 */
public enum PlusTag {
    RED("red", player -> player.discord != null && player.discord.roles != null && player.discord.roles.contains(EAPlayer.Discord.Role.ADMIN)),

    LIGHT_RED("light_red", player -> player.discord != null && player.discord.roles != null && player.discord.roles.contains(EAPlayer.Discord.Role.STAFF)),

    BLUE("blue", player -> player.discord != null && player.discord.roles != null && player.discord.roles.contains(EAPlayer.Discord.Role.CONTRIBUTOR)),

    GOLD("gold", player -> player.subscription != null),

    EMPTY("empty", player -> player.discord != null && player.minecraft != null);


    @NotNull public final String key;
    @NotNull public final Identifier textureId;
    @NotNull public final Predicate<EAPlayer> isUnlocked;

    PlusTag(@NotNull String key, @NotNull Predicate<EAPlayer> isUnlocked) {
        this.key = key;
        this.textureId = Identifier.of(BuildProperties.MOD_ID, "textures/bee/" + key + ".png");
        this.isUnlocked = isUnlocked;
    }
}
