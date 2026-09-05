package cc.aabss.eventutils.versioning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;


public class VersionedClient {
    @Nullable private final Minecraft client;

    public VersionedClient(@Nullable Minecraft client) {
        this.client = client;
    }

    @Nullable
    public Screen screen() {
        if (client == null) return null;

        //? if >=26.2 {
        /*return client.gui.screen();
        *///?} else {
        return client.screen;
        //?}
    }

    public void setScreen(@Nullable Screen screen) {
        if (client == null) return;

        //? if >=26.2 {
        /*client.gui.setScreen(screen);
        *///?} else {
        client.setScreen(screen);
        //?}
    }
}
