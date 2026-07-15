package cc.aabss.eventutils.websocket.listener;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
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
        if (socketEvent.data == null || socketEvent.data.type == null) return;

        // Get event type
        EventType eventType = EventType.fromFamousEventType(socketEvent.data.type);
        if (eventType == null) return;
        if (eventType == EventType.FAMOUS && socketEvent.data.channel != null && socketEvent.data.channel == 1006347642500022353L) eventType = EventType.SKEPPY;

        // Send toast
        if (!mod.config.eventTypes.contains(eventType)) return;
        mod.lastEvent = socketEvent.data;

        // Get and connect to IP
        final String ip = socketEvent.data.message != null ? ConnectUtility.getIp(socketEvent.data.message) : null;
        if (mod.config.autoTp) ConnectUtility.connect(ip == null ? mod.config.defaultFamousIp : ip);

        eventType.sendToast(mod, null, ip != null && !ip.isEmpty());
        mod.lastIps.put(eventType, ip);
        if (ip != null && !ip.isEmpty()) try {
            mod.eventServerManager.addEventServer(eventType, null, "Famous Event", null, ip);
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to add famous event server to server list: {}", socketEvent.data, e);
        }
    }
}
