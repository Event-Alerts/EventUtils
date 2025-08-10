package cc.aabss.eventutils.websocket;

import cc.aabss.eventutils.EventType;
import cc.aabss.eventutils.utility.ConnectUtility;
import cc.aabss.eventutils.EventUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;


public enum SocketEndpoint {
    EVENT_POSTED((mod, message) -> {
        // Get JSON
        final JsonObject json = parseJson(message);
        if (json == null) return;

        // Handle event types
        for (final EventType eventType : EventType.fromJson(json)) {
            if (!mod.config.eventTypes.contains(eventType)) continue;
            LAST_EVENT = json;

            // Get IP and prize amount
            final String ip = mod.getIpAndConnect(eventType, json);
            final int prizeAmount = eventType == EventType.MONEY ? prize(json) : 0;

            // Send toast
            eventType.sendToast(mod, prizeAmount > 0 ? prizeAmount : null, ip != null && !ip.isEmpty());
            mod.lastIps.put(eventType, ip);

            // Add event server to server list if it has an IP
            if (ip != null && !ip.isEmpty()) {
                mod.eventServerManager.addEventServer(json);
            }
        }
    }),
    FAMOUS_EVENT_POSTED((mod, message) -> {
        // Get JSON
        final JsonObject json = parseJson(message);
        if (json == null) return;

        // Get event type
        EventType eventType = EventType.valueOf(json.get("type").getAsString());
        if (eventType.equals(EventType.FAMOUS) && json.get("channel").getAsString().equals("1006347642500022353")) eventType = EventType.SKEPPY;

        // Send toast
        if (!mod.config.eventTypes.contains(eventType)) return;
        LAST_EVENT = json;

        String ip = mod.getIpAndConnect(eventType, json);
        eventType.sendToast(mod, null, ip != null && !ip.isEmpty());
        mod.lastIps.put(eventType, mod.getIpAndConnect(eventType, json));
    }),
    EVENT_CANCELLED((mod, message) -> {
        // Get JSON
        final JsonObject json = parseJson(message);
        if (json == null) return;

        // Remove event server from server list if it exists
        if (json.has("id")) {
            final String eventId = json.get("id").getAsString();
            mod.eventServerManager.removeEventServer(eventId);
        }
    });

    @Nullable public static JsonObject LAST_EVENT;

    @NotNull public final BiConsumer<EventUtils, String> handler;

    SocketEndpoint(@NotNull BiConsumer<EventUtils, String> handler) {
        this.handler = handler;
    }

    @Nullable
    private static JsonObject parseJson(@NotNull String message) {
        try {
            return JsonParser.parseString(message).getAsJsonObject();
        } catch (final Exception e) {
            EventUtils.LOGGER.error("Failed to parse JSON: {}", message);
            return null;
        }
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
