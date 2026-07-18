package cc.aabss.eventutils.websocket.listener;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EventWrapper;
import cc.aabss.eventutils.utility.ConnectUtility;
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
        if (socketEvent.data == null) return;

        // Get event type
        EventType eventType = EventType.fromFamousEventType(socketEvent.data.type);
        if (eventType == null) return;
        if (eventType == EventType.FAMOUS && socketEvent.data.channel != null && socketEvent.data.channel == 1006347642500022353L) eventType = EventType.SKEPPY;
        if (!mod.config.eventTypes.contains(eventType)) return;

        // Set lastEvent
        mod.lastEvent = new EventWrapper(socketEvent.data);

        // Get IP
        String ip = ConnectUtility.getIp(socketEvent.data.message);
        if (ip == null) ip = mod.config.defaultFamousIp;

        // Auto TP if enabled
        if (mod.config.autoTp) ConnectUtility.connect(ip);

        // Send toast
        eventType.sendToast(mod, null, true);
        mod.lastIps.put(eventType, ip);

        // Add event server to server list
        try {
            mod.eventServerManager.addEventServer(eventType, null, "Famous Event", null, ip);
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to add famous event server to server list: {}", socketEvent.data, e);
        }
    }
}
