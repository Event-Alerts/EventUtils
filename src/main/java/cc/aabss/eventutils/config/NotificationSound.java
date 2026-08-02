package cc.aabss.eventutils.config;

import cc.aabss.eventutils.BuildProperties;
import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.text.Text.translatable;


public enum NotificationSound implements NameableEnum {
    NONE,
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

    public void play() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of(BuildProperties.MOD_ID, "notification." + name().toLowerCase())), 1, 1));
    }

    @Override @NotNull @Contract(" -> new")
    public Text getDisplayName() {
        return translatable("eventutils.sound." + name().toLowerCase());
    }
}
