package cc.aabss.eventutils.versioning;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;


public class VersionedLocalPlayer {
    @NotNull private final LocalPlayer player;

    public VersionedLocalPlayer(@NotNull LocalPlayer player) {
        this.player = player;
    }

    public void sendMessage(@NotNull Component component) {
        //? if >=26.1 {
        /*player.sendSystemMessage(component);
        *///?} else {
        player.displayClientMessage(component, false);
        //?}
    }

    public void sendActionBar(@NotNull Component component) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(component);
        *///?} else {
        player.displayClientMessage(component, true);
        //?}
    }
}
