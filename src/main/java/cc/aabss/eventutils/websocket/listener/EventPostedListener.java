package cc.aabss.eventutils.websocket.listener;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EventWrapper;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.websocket.handler.EventPostedHandler;
import gg.eventalerts.sdk.websocket.message.event.SocketEvent;
import org.jetbrains.annotations.NotNull;


public class EventPostedListener extends EventPostedHandler {
    @NotNull private final EventUtils mod;

    public EventPostedListener(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    @Override
    public void onMessage(@NotNull SocketEvent<EAEvent> socketEvent) {
        if (socketEvent.data != null) new EventWrapper(mod, socketEvent.data).executeTypeSettings();
    }
}
