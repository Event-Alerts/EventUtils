package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.manager.EventServerManager;
//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Unique;
*///?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin {
    @Shadow private ServerList servers;
    @Shadow protected ServerSelectionList serverSelectionList;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Store reference to server list for EventServerManager
        EventUtils.MOD.eventServerManager.gotServerList = servers;

        //? if >=1.21.11 {
            /*//? if >=26.1 {
            /^ScreenEvents.afterExtract(
            ^///?} else {
            ScreenEvents.afterRender(
            //?}
                    (Screen) (Object) this).register((screen, context, mouseX, mouseY, delta) -> highlightEventRows(context));
        *///?}
    }

    //? if >=1.21.11 {
    /*@Unique private void highlightEventRows(GuiGraphics context) {
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void highlightEventRows(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    //?}
        // Row-by-row highlight for event servers
        if (serverSelectionList == null) return;
        final int left = serverSelectionList.getRowLeft();
        final int right = left + serverSelectionList.getRowWidth();
        final int servers = serverSelectionList.children().size();
        for (int i = 0; i < servers; i++) {
            final String label = serverSelectionList.children().get(i).getNarration().getString();
            final String normalized = label.replaceAll("§.", "");
            if (!label.contains(EventServerManager.EVENT_SERVER_PREFIX) && !normalized.contains("[Event] ")) continue;

            final int top = ((AbstractSelectionListAccessor) serverSelectionList).invokeGetRowTop(i);
            final int bottom = (i + 1 < servers)
                    ? ((AbstractSelectionListAccessor) serverSelectionList).invokeGetRowTop(i + 1) - 1
                    : top + 36;

            // Subtle highlight overlay so text/icon remain readable
            context.fill(left, top, right, bottom, 0x403575E0);
            // Accent line on the left for emphasis
            context.fill(left, top, left + 2, bottom, 0xFF3575E0);
        }
    }
}
