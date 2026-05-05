package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.EventServerManager;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
        if (EventUtils.MOD != null) {
            EventUtils.MOD.eventServerManager.setServerList(this.serverList);
        }
        ScreenEvents.afterRender((Screen) (Object) this).register((screen, context, mouseX, mouseY, delta) -> highlightEventRows(context));
    }

    private void highlightEventRows(DrawContext context) {
        if (serverListWidget == null) return;
        final int left = serverListWidget.getRowLeft();
        final int right = left + serverListWidget.getRowWidth();
        final int n = serverListWidget.children().size();
        for (int i = 0; i < n; i++) {
            final var entry = serverListWidget.children().get(i);
            final var narration = entry.getNarration();
            if (narration == null) continue;
            final String label = narration.getString();
            final String normalized = label.replaceAll("§.", "");
            if (!label.contains(EventServerManager.EVENT_SERVER_PREFIX) && !normalized.contains("[Event] ")) continue;

            final int top = ((EntryListWidgetAccessor) serverListWidget).invokeGetRowTop(i);
            final int bottom = (i + 1 < n)
                    ? ((EntryListWidgetAccessor) serverListWidget).invokeGetRowTop(i + 1) - 1
                    : top + 36;

            context.fill(left, top, right, bottom, 0x403575E0);
            context.fill(left, top, left + 2, bottom, 0xFF3575E0);
        }
    }
}
