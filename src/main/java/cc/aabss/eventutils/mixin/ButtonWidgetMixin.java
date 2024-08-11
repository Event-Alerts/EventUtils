package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.text.Text.translatable;


@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin extends PressableWidget {
    public ButtonWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void onPress(CallbackInfo ci){
        if (!EventUtils.MOD.config.confirmDisconnect || !translatable("menu.disconnect").equals(getMessage())) return;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !(client.currentScreen instanceof GameMenuScreen)) return;

        ci.cancel();
        final Screen current = client.currentScreen;
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                disconnect();
                return;
            }
            client.setScreen(current);
        }, translatable("eventutils.confirm_disconnect.title"), translatable("eventutils.confirm_disconnect.message")));
    }

    @Unique
    private void disconnect() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;
        final TitleScreen titleScreen = new TitleScreen();
        client.world.disconnect();

        // Singleplayer
        if (client.isInSingleplayer()) {
            client.disconnect(new MessageScreen(translatable("menu.savingLevel")));
            client.setScreen(titleScreen);
            return;
        }
        client.disconnect();

        // Realms
        final ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        //? if <=1.20.1 {
        /*if (serverInfo != null && client.isConnectedToRealms()) {
        *///?} else {
        if (serverInfo != null && serverInfo.isRealm()) {
        //?}
            client.setScreen(new RealmsMainScreen(titleScreen));
            return;
        }

        // Multiplayer
        client.setScreen(new MultiplayerScreen(titleScreen));
    }
}
