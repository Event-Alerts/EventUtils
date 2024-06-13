package cc.aabss.eventutils.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin extends PressableWidget {

    public ButtonWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void onPress(CallbackInfo ci){
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof GameMenuScreen){
            Text text = this.getMessage();
            if (Text.translatable("menu.disconnect").equals(text)){
                if (client.world == null){
                    return;
                }
                ci.cancel();
                Screen current = client.currentScreen;
                client.setScreen(new ConfirmScreen(t -> {
                    if (t){
                        client.disconnect(new MultiplayerScreen(new TitleScreen()));
                    } else {
                        client.setScreen(current);
                    }
                }, Text.literal("Confirm Disconnect"), Text.literal("Are you sure you want to disconnect?")));
            }
        }
    }
}
