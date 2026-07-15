package cc.aabss.eventutils.websocket.listener;

import cc.aabss.eventutils.EventUtils;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.websocket.handler.EventCancelledHandler;
import gg.eventalerts.sdk.websocket.message.event.SocketEvent;
import org.jetbrains.annotations.NotNull;


public class EventCancelledListener extends EventCancelledHandler {
    @NotNull private final EventUtils mod;

    public EventCancelledListener(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    @Override
    public void onMessage(@NotNull SocketEvent<EAEvent> socketEvent) {
        // Remove event server from server list if it exists
        if (socketEvent.data != null && socketEvent.data.id != null) try {
            mod.eventServerManager.removeEventServer(socketEvent.data.id);
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to remove event server from server list: {}", socketEvent, e);
        }
    }
}
