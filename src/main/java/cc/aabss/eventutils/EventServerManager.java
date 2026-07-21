package cc.aabss.eventutils;

import cc.aabss.eventutils.mixin.ServerListAccessor;
import cc.aabss.eventutils.sdk.EventWrapper;
import gg.eventalerts.sdk.object.EAEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


//TODO do not persist changes in servers.dat! causes race conditions and just not necessary.
//     instead, just render event servers in multiplayer screen when screen is rendered (mixin).
public class EventServerManager {
    public static final String EVENT_SERVER_PREFIX = "§7[Event] §r";

    @NotNull private final EventUtils mod;
    @NotNull private final Map<ObjectId, EventServerInfo> activeEventServers = new HashMap<>();
    @NotNull private final Map<ObjectId, ScheduledFuture<?>> removalTasks = new HashMap<>();
    /**
     * Use {@link #getServerList()} instead
     */
    @Nullable public ServerList gotServerList;

    public EventServerManager(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public boolean isActiveEventServer(@NotNull String ip) {
        final String ipLower = ip.toLowerCase();
        return activeEventServers.values().stream().anyMatch(info -> info.serverInfo.address.toLowerCase().equals(ipLower));
    }

    public void addEventServer(@NotNull EventWrapper wrapper) {
        if (wrapper.ip == null) {
            EventUtils.LOGGER.debug("Cannot add event server because IP is null: {}", wrapper);
            return;
        }
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Don't add if already exists
        if (activeEventServers.containsKey(wrapper.id)) return;

        // Get time in milliseconds
        final long timeMillis = wrapper.event instanceof EAEvent eaEvent && eaEvent.time != null ? eaEvent.time.getTime() : System.currentTimeMillis();

        client.execute(() -> {
            final ServerList serverList = getServerList();
            if (serverList == null) {
                EventUtils.LOGGER.warn("Server list not available, cannot add event server");
                return;
            }

            final String serverName = EVENT_SERVER_PREFIX + wrapper.title;

            // Check if server already exists in list
            final ServerInfo existing = serverList.get(wrapper.ip);
            if (existing != null) {
                EventUtils.LOGGER.debug("Event server already present in server list: '{}' -> '{}'", serverName, wrapper.ip);
                return;
            }

            // Create server info
            final ServerInfo serverInfo = new ServerInfo(serverName, wrapper.ip, ServerInfo.ServerType.OTHER);
            serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.PROMPT);

            // Did event start too long ago?
            final long graceMs = TimeUnit.MINUTES.toMillis(mod.config.eventServerDisplayMinutes);
            final long removalDelay = (timeMillis + graceMs) - System.currentTimeMillis();
            if (removalDelay <= 0) {
                EventUtils.LOGGER.debug("Event '{}' started more than {} minutes ago; not adding", serverName, mod.config.eventServerDisplayMinutes);
                return;
            }

            // Mixin to add to top of list
            ((ServerListAccessor) serverList).getServers().add(0, serverInfo); // don't use addFirst to keep older Java version support

            // Store more info ourselves
            activeEventServers.put(wrapper.id, new EventServerInfo(wrapper.id, serverInfo, timeMillis));

            // Schedule removal task
            removalTasks.put(wrapper.id, MiscUtility.IO_SCHEDULER.schedule(() -> removeEventServer(wrapper.id), removalDelay, TimeUnit.MILLISECONDS));

            // Persist changes to disk
            serverList.saveFile();
            EventUtils.LOGGER.debug("Added event server '{}' with IP '{}' to server list, will be removed in {} ms", serverName, wrapper.ip, removalDelay);
        });
    }

    public void removeEventServer(@NotNull ObjectId eventId) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.execute(() -> {
            // Cancel removal task
            final ScheduledFuture<?> removalTask = removalTasks.remove(eventId);
            if (removalTask != null) removalTask.cancel(false);

            // Get and remove EventServerInfo
            final EventServerInfo eventServerInfo = activeEventServers.remove(eventId);
            if (eventServerInfo == null) return;

            // Check if server list loaded
            final ServerList serverList = getServerList();
            if (serverList == null) {
                EventUtils.LOGGER.warn("Server list not available, cannot remove event server");
                return;
            }

            // Remove from server list
            serverList.remove(eventServerInfo.serverInfo);

            // Persist removal
            serverList.saveFile();
            EventUtils.LOGGER.debug("Removed event server from server list: {}", eventServerInfo.serverInfo.name);
        });
    }

    public void removeAllEventServers() {
        // Remove all event servers
        for (final ObjectId eventId : new HashMap<>(activeEventServers).keySet()) removeEventServer(eventId);

        // Ensure all removal tasks cancelled
        removalTasks.values().forEach(task -> task.cancel(false));
        removalTasks.clear();
    }

    @Nullable
    private ServerList getServerList() {
        if (gotServerList != null) return gotServerList;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;
        gotServerList = new ServerList(client);
        try {
            gotServerList.loadFile();
        } catch (final Exception e) {
            EventUtils.LOGGER.error("Failed to load server list from file", e);
        }
        return gotServerList;
    }

    private record EventServerInfo(@NotNull ObjectId eventId, @NotNull ServerInfo serverInfo, long eventTime) {}
}
