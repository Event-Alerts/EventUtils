package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
//? if >=1.21.11
//import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.text.Text.translatable;


@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin extends PressableWidget {
    public ButtonWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    //? if >=1.21.11 {
    /*@Shadow public abstract void onPress(AbstractInput abstractInput);
    *///?} else {
    @Shadow public abstract void onPress();
    //?}

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.11 {
    /*public void onPress(AbstractInput abstractInput, CallbackInfo ci) {
    *///?} else {
    private void onPress(CallbackInfo ci) {
    //?}
        if (!EventUtils.MOD.config.confirmDisconnect || !translatable("menu.disconnect").equals(getMessage())) return;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !(client.currentScreen instanceof GameMenuScreen)) return;

        ci.cancel();
        final Screen current = client.currentScreen;
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                //? if >=1.21.11 {
                /*this.onPress(abstractInput);
                *///?} else {
                this.onPress();
                //?}
                return;
            }
            client.setScreen(current);
        }, translatable("eventutils.confirm_disconnect.title"), translatable("eventutils.confirm_disconnect.message")));
    }
}