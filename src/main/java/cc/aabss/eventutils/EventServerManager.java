package cc.aabss.eventutils;

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
    public static final String EVENT_SERVER_PREFIX = "§7[Event] §r";

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

    public void addEventServer(@NotNull JsonObject eventJson, @NotNull String ip) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Get ID
        String eventId = "event-" + System.currentTimeMillis();
        if (eventJson.has("id")) try {
            String idJson = eventJson.get("id").getAsString();
            if (idJson != null && !idJson.isEmpty()) eventId = idJson;
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to parse ID from event: {}", eventJson, e);
        }
        final String finalEventId = eventId; // Requires final variable due to lambda

        // Don't add if already exists (fast-path check)
        if (activeEventServers.containsKey(eventId)) return;

        // Get title
        String title = "Event";
        if (eventJson.has("title")) try {
            String titleJson = eventJson.get("title").getAsString();
            if (titleJson != null && !titleJson.isEmpty()) title = titleJson;
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to parse title from event: {}", eventJson, e);
        }
        final String finalTitle = title; // Requires final variable due to lambda

        // Get time
        long eventTime = System.currentTimeMillis();
        if (eventJson.has("time")) try {
            long eventTimeJson = eventJson.get("time").getAsLong();
            if (eventTimeJson > 0) eventTime = eventTimeJson;
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to parse time from event: {}", eventJson, e);
        }
        final long finalEventTime = eventTime; // Requires final variable due to lambda

        client.execute(() -> {
            if (!ensureServerListLoaded()) {
                EventUtils.LOGGER.warn("Server list not available, cannot add event server");
                return;
            }
            if (serverList == null) return;

            // Create server info
            final String serverName = EVENT_SERVER_PREFIX + finalTitle;
            final ServerInfo serverInfo = new ServerInfo(serverName, ip, ServerInfo.ServerType.OTHER);
            serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.PROMPT);

            // Add the server to the list (avoid duplicates in the persistent list)
            for (int i = 0; i < serverList.size(); i++) {
                final ServerInfo existing = serverList.get(i);
                if (existing.name.equals(serverName) && existing.address.equalsIgnoreCase(ip)) {
                    EventUtils.LOGGER.info("Event server already present in server list: '{}' -> '{}'", serverName, ip);
                    return;
                }
            }
            serverList.add(serverInfo, false);

            // Store event server info
            final EventServerInfo eventServerInfo = new EventServerInfo(finalEventId, serverInfo, finalEventTime);
            activeEventServers.put(finalEventId, eventServerInfo);

            // Schedule removal 5 minutes after event starts
            final long currentTime = System.currentTimeMillis();
            final long graceMs = TimeUnit.MINUTES.toMillis(5);
            final long timeUntilRemoval = (finalEventTime + graceMs) - currentTime;

            if (timeUntilRemoval > 0) {
                final ScheduledFuture<?> removalTask = mod.scheduler.schedule(
                    () -> removeEventServer(finalEventId),
                    timeUntilRemoval,
                    TimeUnit.MILLISECONDS);
                removalTasks.put(finalEventId, removalTask);
                EventUtils.LOGGER.info("Scheduled removal of event server '{}' in {} ms (5m after start)", finalTitle, timeUntilRemoval);
            } else {
                // If within 5-minute grace after event start, keep it briefly; else do not add
                if (currentTime - finalEventTime <= graceMs) {
                    final long remaining = graceMs - (currentTime - finalEventTime);
                    final ScheduledFuture<?> removalTask = mod.scheduler.schedule(
                            () -> removeEventServer(finalEventId),
                            remaining,
                            TimeUnit.MILLISECONDS);
                    removalTasks.put(finalEventId, removalTask);
                    EventUtils.LOGGER.info("Event '{}' already started; keeping for {} ms (grace)", finalTitle, remaining);
                } else {
                    serverList.remove(serverInfo);
                    activeEventServers.remove(finalEventId);
                    EventUtils.LOGGER.info("Event '{}' started more than 5 minutes ago; not adding", finalTitle);
                    return;
                }
            }

            // Persist changes to disk so they show up when user opens the Multiplayer screen later
            try {
                serverList.saveFile();
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to save server list after adding event server", e);
            }

            EventUtils.LOGGER.info("Added event server '{}' with IP '{}' to server list", finalTitle, ip);
        });
    }

    public void removeEventServer(@NotNull String eventId) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.execute(() -> {
            final EventServerInfo eventServerInfo = activeEventServers.remove(eventId);
            if (eventServerInfo == null) return;

            if (!ensureServerListLoaded()) {
                EventUtils.LOGGER.warn("Server list not available, cannot remove event server");
                return;
            }
            if (serverList == null) return;

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

    public int getActiveEventCount() {
        return activeEventServers.size();
    }

    private boolean ensureServerListLoaded() {
        if (this.serverList != null) return true;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        this.serverList = new ServerList(client);
        try {
            this.serverList.loadFile();
        } catch (final Exception e) {
            EventUtils.LOGGER.error("Failed to load server list from file", e);
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
