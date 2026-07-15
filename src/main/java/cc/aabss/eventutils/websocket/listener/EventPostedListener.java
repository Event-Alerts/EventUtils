package cc.aabss.eventutils.websocket.listener;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.utility.ConnectUtility;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.websocket.handler.EventPostedHandler;
import gg.eventalerts.sdk.websocket.message.event.SocketEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class EventPostedListener extends EventPostedHandler {
    @NotNull private final EventUtils mod;

    public EventPostedListener(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    @Override
    public void onMessage(@NotNull SocketEvent<EAEvent> socketEvent) {
        if (socketEvent.data == null || socketEvent.data.rolesNamed == null) return;
        for (final EAEvent.PingRole pingRole : socketEvent.data.rolesNamed) {
            final EventType eventType = EventType.fromPingRole(pingRole);
            if (eventType == null || !mod.config.eventTypes.contains(eventType)) continue;
            mod.lastEvent = socketEvent.data;

            // Get IP
            String ip = null;
            if (eventType == EventType.HOUSING) {
                ip = "hypixel.net";
            } else {
                final String extracted = extractIp(socketEvent.data);
                if (extracted != null) ip = extracted;
            }
            final boolean hasIp = ip != null;

            // Auto TP if enabled
            if (mod.config.autoTp && hasIp) ConnectUtility.connect(ip);

            // Send toast
            final int prizeAmount = eventType == EventType.MONEY ? prize(socketEvent.data) : 0;
            eventType.sendToast(mod, prizeAmount > 0 ? prizeAmount : null, hasIp);
            mod.lastIps.put(eventType, ip);

            // Add event server to server list if it has an IP
            if (hasIp) try {
                mod.eventServerManager.addEventServer(eventType, socketEvent.data.id, socketEvent.data.title, socketEvent.data.time, ip);
            } catch (final Exception e) {
                EventUtils.LOGGER.warn("Failed to add event server to server list: {}", socketEvent.data, e);
            }
        }
    }

    @Nullable
    private static String extractIp(@NotNull EAEvent event) {
        // Direct IP field
        if (event.ip != null && !event.ip.isEmpty()) return event.ip;

        // Extract from description
        if (event.description != null) {
            final String extracted = ConnectUtility.getIp(event.description);
            if (extracted != null && !extracted.isEmpty()) return extracted;
        }

        // Extract from title
        if (event.title != null) {
            final String extracted = ConnectUtility.getIp(event.title);
            if (extracted != null && !extracted.isEmpty()) return extracted;
        }

        return null;
    }

    private static int prize(@NotNull EAEvent event) {
        // Get prize from JSON
        if (event.prize != null) return Integer.parseInt(event.prize.replaceAll("[$€£]", "").split(" ")[0]);

        // Get description
        if (event.description == null) return 0;

        // Extract prize from description
        for (final String line : ConnectUtility.removeMarkdown(event.description.toLowerCase()).split("\\n+")) {
            if (!line.contains("$") && !line.contains("€") && !line.contains("£") && !line.contains("dollars") && !line.contains("prize")) continue;

            for (String word : line.split(" ")) {
                if (word.contains("$") || word.contains("€") || word.contains("£")) word = word.replaceAll("[$€£]", "");
                try {
                    return Integer.parseInt(word);
                } catch (final NumberFormatException ignored) {}
            }
        }
        return 0;
    }
}
