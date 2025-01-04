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
        boolean setLastEvent = false;
        for (final EventType eventType : EventType.fromJson(json)) {
            if (!mod.config.eventTypes.contains(eventType)) continue;
            setLastEvent = true;
            eventType.sendToast(eventType == EventType.MONEY ? prize(json) : null);
            mod.lastIps.put(eventType, mod.getIpAndConnect(eventType, json));
        }
        if (setLastEvent) lastEvent = json;
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
        lastEvent = json;
        eventType.sendToast(null);
        mod.lastIps.put(eventType, mod.getIpAndConnect(eventType, json));
    });

    public static JsonObject lastEvent;
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
