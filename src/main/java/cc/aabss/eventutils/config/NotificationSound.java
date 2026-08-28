package cc.aabss.eventutils.config;

import cc.aabss.eventutils.BuildProperties;
import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.network.chat.Component.translatable;


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
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BuildProperties.MOD_ID, "notification." + name().toLowerCase())), 1, 1));
    }

    @Override @NotNull @Contract(" -> new")
    public Component getDisplayName() {
        return translatable("eventutils.sound." + name().toLowerCase());
    }
}
