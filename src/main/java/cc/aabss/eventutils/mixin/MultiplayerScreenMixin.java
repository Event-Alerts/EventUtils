package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.EventServerManager;
//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Unique;
*///? } else {
import net.minecraft.client.gui.DrawContext;
//?}
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {

    @Shadow private ServerList serverList;
    @Shadow protected MultiplayerServerListWidget serverListWidget;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Store reference to server list for EventServerManager
        EventUtils.MOD.eventServerManager.setServerList(this.serverList);

        //? if >=1.21.11
        //ScreenEvents.afterRender((Screen) (Object) this).register((screen, context, mouseX, mouseY, delta) -> highlightEventRows(context));
    }

    //? if >=1.21.11 {
    /*@Unique private void highlightEventRows(DrawContext context) {
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void highlightEventRows(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    //?}
        if (serverListWidget == null) return;
        // Row-by-row highlight for event servers
        final int left = serverListWidget.getRowLeft();
        final int right = left + serverListWidget.getRowWidth();
        final int servers = serverListWidget.children().size();
        for (int i = 0; i < servers; i++) {
            final var entry = serverListWidget.children().get(i);
            final var narration = entry.getNarration();
            if (narration == null) continue;
            final String label = narration.getString();
            final String normalized = label.replaceAll("§.", "");
            if (!label.contains(EventServerManager.EVENT_SERVER_PREFIX) && !normalized.contains("[Event] ")) continue;

            final int top = ((EntryListWidgetAccessor) serverListWidget).invokeGetRowTop(i);
            final int bottom = (i + 1 < servers)
                    ? ((EntryListWidgetAccessor) serverListWidget).invokeGetRowTop(i + 1) - 1
                    : top + 36;

            // Subtle highlight overlay so text/icon remain readable
            context.fill(left, top, right, bottom, 0x403575E0);
            // Accent line on the left for emphasis
            context.fill(left, top, left + 2, bottom, 0xFF3575E0);
        }
    }
}
