package cc.aabss.eventutils.versioning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class VersionedClient {
    @NotNull private final Minecraft client;

    public VersionedClient(@NotNull Minecraft client) {
        this.client = client;
    }

    @Nullable
    public Screen screen() {
        //? if >=26.2 {
        /*return client.gui.screen();
        *///?} else {
        return client.screen;
        //?}
    }

    public void setScreen(@Nullable Screen screen) {
        //? if >=26.2 {
        /*client.gui.setScreen(screen);
        *///?} else {
        client.setScreen(screen);
        //?}
    }
}
