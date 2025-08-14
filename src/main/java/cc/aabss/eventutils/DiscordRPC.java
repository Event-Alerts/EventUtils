package cc.aabss.eventutils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import org.json.JSONObject;


public class DiscordRPC {
    private static final OffsetDateTime START = OffsetDateTime.now();

    @NotNull private final EventUtils mod;
    @Nullable private ScheduledFuture<?> presenceTask;
    @Nullable public IPCClient client;

    public DiscordRPC(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void connect() {
        if (!mod.config.discordRpc) return;
        if (client != null && client.getStatus() == PipeStatus.CONNECTED) return;

        client = new IPCClient(1351016544779374735L);
        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {
                EventUtils.LOGGER.info("DISCORD RPC: Connected");
                scheduleUpdates();
            }

            @Override
            public void onClose(IPCClient client, JSONObject json) {
                EventUtils.LOGGER.info("DISCORD RPC: Disconnected ({})", json);
                cancelUpdates();
            }

            @Override
            public void onDisconnect(IPCClient client, Throwable t) {
                EventUtils.LOGGER.warn("DISCORD RPC: Disconnected due to error", t);
                cancelUpdates();
            }
        });

        try {
            client.connect();
        } catch (final Exception e) {
            EventUtils.LOGGER.error("DISCORD RPC: Failed to connect", e);
            client = null;
        }
    }

    public void disconnect() {
        cancelUpdates();
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {}
        }
        client = null;
    }

    private void scheduleUpdates() {
        cancelUpdates();
        presenceTask = mod.scheduler.scheduleAtFixedRate(this::updatePresence, 0, 10, TimeUnit.SECONDS);
    }

    private void cancelUpdates() {
        if (presenceTask != null) {
            presenceTask.cancel(false);
            presenceTask = null;
        }
    }

    private void updatePresence() {
        if (!mod.config.discordRpc) return;
        if (client == null || client.getStatus() != PipeStatus.CONNECTED) return;

        final Status status = Status.get();
        final String username = MinecraftClient.getInstance().getSession().getUsername();
        final RichPresence.Builder builder = new RichPresence.Builder()
                .setState("Currently in " + status.text)
                .setDetails("Playing as " + username)
                .setStartTimestamp(START)
                .setLargeImage("logo", "EventUtils" + (Versions.EU_VERSION != null ? " v" + Versions.EU_VERSION : ""))
                .setSmallImage("minecraft", "Minecraft" + (Versions.MC_VERSION != null ? " v" + Versions.MC_VERSION : ""));

        // These dont work ig
//        builder.set("Download the mod!", "https://modrinth.com/mod/alerts");
//        builder.setButton2("Join the Discord!", "https://discord.gg/aGDuQcduWZ");

        try {
            client.sendRichPresence(builder.build());
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("DISCORD RPC: Failed to update presence", e);
        }
    }

    private enum Status {
        SINGLEPLAYER("Singleplayer"),
        MULTIPLAYER("Multiplayer"),
        MAIN_MENU("the Main Menu");

        private final String text;

        Status(String text) {
            this.text = text;
        }

        @NotNull
        private static Status get() {
            final MinecraftClient client = MinecraftClient.getInstance();
            final IntegratedServer server = client.getServer();
            if (server != null && server.isRunning()) return SINGLEPLAYER;
            final ServerInfo entry = client.getCurrentServerEntry();
            if (entry != null && entry.address != null && !Objects.equals(entry.address, "")) return MULTIPLAYER;
            return MAIN_MENU;
        }
    }
}


