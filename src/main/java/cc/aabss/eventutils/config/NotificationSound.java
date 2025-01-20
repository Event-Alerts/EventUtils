package cc.aabss.eventutils.config;

import dev.isxander.yacl3.api.NameableEnum;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.text.Text.translatable;


public enum NotificationSound implements NameableEnum {
    ALARM,
    ALERT,
    CALM,
    CAT,
    CHIME,
    GOOFY,
    PLUCK,
    REVERB,
    SHAKEY,
    TIME_OF_WAR;

    @NotNull
    public Identifier getIdentifier() {
        return Identifier.of("eventutils", "notification." + name().toLowerCase());
    }

    @Override @NotNull @Contract(" -> new")
    public Text getDisplayName() {
        return translatable("eventutils.sound." + name().toLowerCase());
    }

    @Nullable
    public static NotificationSound fromString(@NotNull String string) {
        try {
            return valueOf(string.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
}
