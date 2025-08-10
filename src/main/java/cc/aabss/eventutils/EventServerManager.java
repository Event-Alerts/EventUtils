package cc.aabss.eventutils;

import cc.aabss.eventutils.utility.ConnectUtility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EventServerManager {
    private static final String EVENT_SERVER_PREFIX = "§7[Event] §r";
    private static final String EVENTS_DIVIDER_NAME = "§8--- Events ---";

    @NotNull private final EventUtils mod;
    @NotNull private final Map<String, EventServerInfo> activeEventServers = new HashMap<>();
    @NotNull private final Map<String, ScheduledFuture<?>> removalTasks = new HashMap<>();
    @Nullable private ServerList serverList;

    public EventServerManager(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void setServerList(@Nullable ServerList serverList) {
        this.serverList = serverList;
    }

    public void addEventServer(@NotNull JsonObject eventJson) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Extract event information
        final String eventId = eventJson.has("id") ? eventJson.get("id").getAsString() : ("event-" + System.currentTimeMillis());
        final String title = eventJson.has("title") ? eventJson.get("title").getAsString() : "Event";
        final long eventTime = eventJson.has("time") ? eventJson.get("time").getAsLong() : 0;

        // Try to extract server IP from various possible fields
        String serverIp = extractServerIp(eventJson);
        if (serverIp == null || serverIp.isEmpty()) {
            EventUtils.LOGGER.warn("No server IP found for event: {}", title);
            return;
        }

        // Don't add if already exists (fast-path check)
        if (activeEventServers.containsKey(eventId)) return;

        client.execute(() -> {
            if (!ensureServerListLoaded()) {
                EventUtils.LOGGER.warn("Server list not available, cannot add event server");
                return;
            }

            // Create server info
            final String serverName = EVENT_SERVER_PREFIX + title;
            final ServerInfo serverInfo = new ServerInfo(serverName, serverIp, ServerInfo.ServerType.OTHER);
            serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.PROMPT);

            // Add events divider if it doesn't exist
            addEventsDividerIfNeeded(serverList);

            // Add the server to the list (avoid duplicates in the persistent list)
            for (int i = 0; i < serverList.size(); i++) {
                final ServerInfo existing = serverList.get(i);
                if (existing.name.equals(serverName) && existing.address.equalsIgnoreCase(serverIp)) {
                    EventUtils.LOGGER.info("Event server already present in server list: '{}' -> '{}'", serverName, serverIp);
                    return;
                }
            }
            serverList.add(serverInfo, false);

            // Store event server info
            final EventServerInfo eventServerInfo = new EventServerInfo(eventId, serverInfo, eventTime);
            activeEventServers.put(eventId, eventServerInfo);

            // Schedule removal when event starts
            if (eventTime > 0) {
                final long currentTime = System.currentTimeMillis();
                final long timeUntilEvent = eventTime - currentTime;

                if (timeUntilEvent > 0) {
                    final ScheduledFuture<?> removalTask = mod.scheduler.schedule(
                        () -> removeEventServer(eventId),
                        timeUntilEvent,
                        TimeUnit.MILLISECONDS
                    );
                    removalTasks.put(eventId, removalTask);
                    EventUtils.LOGGER.info("Scheduled removal of event server '{}' in {} ms", title, timeUntilEvent);
                } else {
                    // Event has already started, don't add it
                    serverList.remove(serverInfo);
                    activeEventServers.remove(eventId);
                    EventUtils.LOGGER.info("Event '{}' has already started, not adding to server list", title);
                    return;
                }
            }

            // Persist changes to disk so they show up when user opens the Multiplayer screen later
            try {
                serverList.saveFile();
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to save server list after adding event server", e);
            }

            EventUtils.LOGGER.info("Added event server '{}' with IP '{}' to server list", title, serverIp);
        });
    }

    public void removeEventServer(@NotNull String eventId) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        client.execute(() -> {
            final EventServerInfo eventServerInfo = activeEventServers.remove(eventId);
            if (eventServerInfo == null) return;

            if (!ensureServerListLoaded()) {
                EventUtils.LOGGER.warn("Server list not available, cannot remove event server");
                return;
            }

            // Remove from server list by matching properties (instance may differ if server list was reloaded)
            int removedCount = 0;
            for (int i = serverList.size() - 1; i >= 0; i--) {
                final ServerInfo candidate = serverList.get(i);
                if (candidate.name.equals(eventServerInfo.serverInfo.name)
                        && candidate.address.equalsIgnoreCase(eventServerInfo.serverInfo.address)) {
                    serverList.remove(candidate);
                    removedCount++;
                }
            }
            if (removedCount == 0) {
                EventUtils.LOGGER.warn("Event server not found in current server list for removal: '{}' -> '{}'", eventServerInfo.serverInfo.name, eventServerInfo.serverInfo.address);
            }

            // Cancel removal task
            final ScheduledFuture<?> removalTask = removalTasks.remove(eventId);
            if (removalTask != null) {
                removalTask.cancel(false);
            }

            // Remove events divider if no more event servers
            if (activeEventServers.isEmpty()) {
                removeEventsDivider(serverList);
            }

            // Persist removal
            try {
                serverList.saveFile();
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to save server list after removing event server", e);
            }

            EventUtils.LOGGER.info("Removed event server from server list: {}", eventServerInfo.serverInfo.name);
        });
    }

    public void removeAllEventServers() {
        // Cancel all removal tasks
        removalTasks.values().forEach(task -> task.cancel(false));
        removalTasks.clear();

        // Remove all event servers
        for (final String eventId : new HashMap<>(activeEventServers).keySet()) {
            removeEventServer(eventId);
        }
    }

    @Nullable
    private String extractServerIp(@NotNull JsonObject eventJson) {
        // Try to get IP from common fields in the event JSON
        if (eventJson.has("ip")) {
            return eventJson.get("ip").getAsString();
        }
        if (eventJson.has("server")) {
            return eventJson.get("server").getAsString();
        }
        if (eventJson.has("address")) {
            return eventJson.get("address").getAsString();
        }

        // Try to extract from description using the existing utility
        if (eventJson.has("description")) {
            final String description = eventJson.get("description").getAsString();
            final String extractedIp = ConnectUtility.getIp(description);
            if (extractedIp != null && !extractedIp.isEmpty()) {
                return extractedIp;
            }
        }

        // Try to extract from title as well
        if (eventJson.has("title")) {
            final String title = eventJson.get("title").getAsString();
            final String extractedIp = ConnectUtility.getIp(title);
            if (extractedIp != null && !extractedIp.isEmpty()) {
                return extractedIp;
            }
        }

        // No IP could be determined
        return null;
    }

    private void addEventsDividerIfNeeded(@NotNull ServerList serverList) {
        // Check if divider already exists
        for (int i = 0; i < serverList.size(); i++) {
            final ServerInfo server = serverList.get(i);
            if (server.name.equals(EVENTS_DIVIDER_NAME)) {
                return; // Divider already exists
            }
        }

        // Add divider at the end
        final ServerInfo divider = new ServerInfo(EVENTS_DIVIDER_NAME, "", ServerInfo.ServerType.OTHER);
        serverList.add(divider, false);
    }

    private void removeEventsDivider(@NotNull ServerList serverList) {
        for (int i = 0; i < serverList.size(); i++) {
            final ServerInfo server = serverList.get(i);
            if (server.name.equals(EVENTS_DIVIDER_NAME)) {
                serverList.remove(server);
                return;
            }
        }
    }

    private boolean ensureServerListLoaded() {
        if (this.serverList != null) return true;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        this.serverList = new ServerList(client);
        try {
            this.serverList.loadFile();
        } catch (final Exception e) {
            EventUtils.LOGGER.error("Failed to load server list", e);
        }
        return true;
    }

    private static class EventServerInfo {
        @NotNull public final String eventId;
        @NotNull public final ServerInfo serverInfo;
        public final long eventTime;

        public EventServerInfo(@NotNull String eventId, @NotNull ServerInfo serverInfo, long eventTime) {
            this.eventId = eventId;
            this.serverInfo = serverInfo;
            this.eventTime = eventTime;
        }
    }
}
