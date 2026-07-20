package cc.aabss.eventutils.websocket.listener;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EventWrapper;
import gg.eventalerts.sdk.object.EAFamousEvent;
import gg.eventalerts.sdk.websocket.handler.FamousEventPostedHandler;
import gg.eventalerts.sdk.websocket.message.event.SocketEvent;
import org.jetbrains.annotations.NotNull;


public class FamousEventPostedListener extends FamousEventPostedHandler {
    @NotNull private final EventUtils mod;

    public FamousEventPostedListener(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    @Override
    public void onMessage(@NotNull SocketEvent<EAFamousEvent> socketEvent) {
        if (socketEvent.data != null) new EventWrapper(mod, socketEvent.data).executeTypeSettings();
    }
}
