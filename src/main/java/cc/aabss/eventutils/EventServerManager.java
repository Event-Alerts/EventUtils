package cc.aabss.eventutils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class EventServerManager {
    public static final String EVENT_SERVER_PREFIX = "§7[Event] §r";

    @NotNull private final EventUtils mod;
    @NotNull private final Map<ObjectId, EventServerInfo> activeEventServers = new HashMap<>();
    @NotNull private final Map<ObjectId, ScheduledFuture<?>> removalTasks = new HashMap<>();
    @Nullable private ServerList serverList;

    public EventServerManager(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void setServerList(@Nullable ServerList serverList) {
        this.serverList = serverList;
    }

    public void addEventServer(@NotNull EventType eventType, @Nullable ObjectId id, @Nullable String title, @Nullable Date time, @NotNull String ip) {
        if (!mod.config.eventServersEnabled || !mod.config.eventServerTypes.contains(eventType)) return;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Finalize ID
        if (id == null) id = new ObjectId();
        final ObjectId finalId = id;

        // Don't add if already exists (fast-path check)
        if (activeEventServers.containsKey(id)) return;

        // Finalize title
        if (title == null) title = eventType.name();
        final String finalTitle = title;

        // Get time in milliseconds
        final long timeMillis = time != null ? time.getTime() : System.currentTimeMillis();

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
                    EventUtils.LOGGER.debug("Event server already present in server list: '{}' -> '{}'", serverName, ip);
                    return;
                }
            }
            serverList.add(serverInfo, false);

            // Store event server info
            final EventServerInfo eventServerInfo = new EventServerInfo(finalId, serverInfo, timeMillis);
            activeEventServers.put(finalId, eventServerInfo);

            // Schedule removal after configurable grace period (default 5 minutes)
            final long currentTime = System.currentTimeMillis();
            final long graceMs = TimeUnit.MINUTES.toMillis(mod.config.eventServerDisplayMinutes);
            final long timeUntilRemoval = (timeMillis + graceMs) - currentTime;

            if (timeUntilRemoval > 0) {
                final ScheduledFuture<?> removalTask = MiscUtility.IO_SCHEDULER.schedule(() -> removeEventServer(finalId), timeUntilRemoval, TimeUnit.MILLISECONDS);
                removalTasks.put(finalId, removalTask);
                EventUtils.LOGGER.debug("Scheduled removal of event server '{}' in {} ms ({}m after start)", finalTitle, timeUntilRemoval, mod.config.eventServerDisplayMinutes);
            } else {
                // If within grace period after event start, keep it briefly; else do not add
                if (currentTime - timeMillis <= graceMs) {
                    final long remaining = graceMs - (currentTime - timeMillis);
                    final ScheduledFuture<?> removalTask = MiscUtility.IO_SCHEDULER.schedule(() -> removeEventServer(finalId), remaining, TimeUnit.MILLISECONDS);
                    removalTasks.put(finalId, removalTask);
                    EventUtils.LOGGER.debug("Event '{}' already started; keeping for {} ms ({}m grace)", finalTitle, remaining, mod.config.eventServerDisplayMinutes);
                } else {
                    serverList.remove(serverInfo);
                    activeEventServers.remove(finalId);
                    EventUtils.LOGGER.debug("Event '{}' started more than {} minutes ago; not adding", finalTitle, mod.config.eventServerDisplayMinutes);
                    return;
                }
            }

            // Persist changes to disk so they show up when user opens the Multiplayer screen later
            try {
                serverList.saveFile();
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to save server list after adding event server", e);
            }

            EventUtils.LOGGER.debug("Added event server '{}' with IP '{}' to server list", finalTitle, ip);
        });
    }

    public void removeEventServer(@NotNull ObjectId eventId) {
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
                if (candidate.name.equals(eventServerInfo.serverInfo.name) && candidate.address.equalsIgnoreCase(eventServerInfo.serverInfo.address)) {
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

            EventUtils.LOGGER.debug("Removed event server from server list: {}", eventServerInfo.serverInfo.name);
        });
    }

    public void removeAllEventServers() {
        // Cancel all removal tasks
        removalTasks.values().forEach(task -> task.cancel(false));
        removalTasks.clear();

        // Remove all event servers
        for (final ObjectId eventId : new HashMap<>(activeEventServers).keySet()) {
            removeEventServer(eventId);
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
            EventUtils.LOGGER.error("Failed to load server list from file", e);
        }
        return true;
    }

    private static class EventServerInfo {
        @NotNull public final ObjectId eventId;
        @NotNull public final ServerInfo serverInfo;
        public final long eventTime;

        public EventServerInfo(@NotNull ObjectId eventId, @NotNull ServerInfo serverInfo, long eventTime) {
            this.eventId = eventId;
            this.serverInfo = serverInfo;
            this.eventTime = eventTime;
        }
    }
}
