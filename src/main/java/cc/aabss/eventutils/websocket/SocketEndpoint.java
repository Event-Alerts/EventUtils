package cc.aabss.eventutils.websocket;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.ConnectUtility;
import cc.aabss.eventutils.EventUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;


public enum SocketEndpoint {
    EVENT_POSTED((mod, message) -> {
        // Get JSON
        final JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            EventUtils.LOGGER.error("Failed to parse JSON: {}", message);
            return;
        }
        // Handle event types
        for (final EventType eventType : EventType.fromJson(json)) {
            if (!mod.config.eventTypes.contains(eventType)) return;
            eventType.sendToast(eventType == EventType.MONEY ? prize(json) : null);
            mod.lastIps.put(eventType, mod.getIpAndConnect(eventType, json));
        }
    }),
    FAMOUS_EVENT((mod, message) -> {
        if (!mod.config.eventTypes.contains(EventType.FAMOUS)) return;
        EventType.FAMOUS.sendToast(null);
        mod.lastIps.put(EventType.FAMOUS, mod.getIpAndConnect(EventType.FAMOUS, message));
    }),
    POTENTIAL_FAMOUS_EVENT((mod, message) -> {
        if (!mod.config.eventTypes.contains(EventType.POTENTIAL_FAMOUS)) return;
        EventType.POTENTIAL_FAMOUS.sendToast(null);
        mod.lastIps.put(EventType.POTENTIAL_FAMOUS, mod.getIpAndConnect(EventType.POTENTIAL_FAMOUS, message));
    });

    @NotNull public final BiConsumer<EventUtils, String> handler;

    SocketEndpoint(@NotNull BiConsumer<EventUtils, String> handler) {
        this.handler = handler;
    }

    private static int prize(@NotNull JsonObject event) {
        // Get prize from JSON
        final JsonElement prize = event.get("prize");
        if (prize != null) return Integer.parseInt(prize.getAsString().replaceAll("[$€£]", "").split(" ")[0]);

        // Get description
        final JsonElement description = event.get("description");
        if (description == null) return 0;

        // Extract prize from description
        for (final String line : ConnectUtility.removeMarkdown(description.getAsString().toLowerCase()).split("\\n+")) {
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
