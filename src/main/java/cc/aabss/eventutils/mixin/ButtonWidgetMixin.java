package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.11 {
/*import net.minecraft.client.input.InputWithModifiers;
import org.jetbrains.annotations.NotNull;
*///?}

import static net.minecraft.network.chat.Component.translatable;


@Mixin(Button.class)
public abstract class ButtonWidgetMixin extends AbstractButton {
    public ButtonWidgetMixin(int i, int j, int k, int l, Component text) {
        super(i, j, k, l, text);
    }

    //? if >=1.21.11 {
    /*@Shadow public abstract void onPress(@NotNull InputWithModifiers abstractInput);
    *///?} else {
    @Shadow public abstract void onPress();
    //?}

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.11 {
    /*public void onPress(InputWithModifiers abstractInput, CallbackInfo ci) {
    *///?} else {
    private void onPress(CallbackInfo ci) {
    //?}
        if (!EventUtils.MOD.config.confirm_disconnect || !translatable("menu.disconnect").equals(getMessage())) return;
        final Minecraft client = Minecraft.getInstance();
        if (client.level == null || !(client.screen instanceof PauseScreen)) return;

        ci.cancel();
        final Screen current = client.screen;
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