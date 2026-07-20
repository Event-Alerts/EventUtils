package cc.aabss.eventutils.discordrpc;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.EventUtils;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import gg.eventalerts.sdk.http.action.EAAction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;


public class DiscordRPC {
    @NotNull public static final Duration REFRESH_INTERVAL = Duration.ofSeconds(30);
    private static final long START = System.currentTimeMillis();

    @NotNull private final EventUtils mod;
    @NotNull private final IPCClient client = new IPCClient(1351016544779374735L);
    @Nullable private Status status;
    @Nullable private String url;

    public DiscordRPC(@NotNull EventUtils mod) {
        this.mod = mod;
        this.client.setListener(new CustomIPCListener(this));
        refreshConnection();
    }

    public void refreshConnection() {
        final PipeStatus status = client.getStatus();
        EventUtils.LOGGER.debug("[DISCORD RPC] config.discordRpc={} status={}", mod.config.discordRpc, status);

        // Enable
        if (mod.config.discordRpc) {
            // Already enabled
            if (status == PipeStatus.CONNECTING || status == PipeStatus.CONNECTED) return;

            // Enable
            try {
                EventUtils.LOGGER.debug("[DISCORD RPC] Connecting...");
                client.connect();
                EventUtils.LOGGER.debug("[DISCORD RPC] Connected");
            } catch (final Exception e) {
                EventUtils.LOGGER.warn("[DISCORD RPC] Failed to connect!", e);
            }
            return;
        }

        // Already disabled
        if (status == PipeStatus.CLOSING || status == PipeStatus.CLOSED || status == PipeStatus.DISCONNECTED) return;

        // Disable
        close();
    }

    public void close() {
        client.close();
    }

    public void updatePresence() {
        // Retrieve URL then send presence
        if (url == null) {
            retrieveUrl(MinecraftClient.getInstance().getSession().getUuidOrNull()).queue(url -> {
                this.url = url;
                sendPresence();
            });
            return;
        }

        // Send presence immediately with existing URL
        sendPresence();
    }

    private void sendPresence() {
        // Update status
        final boolean statusChanged = updateStatus();
        EventUtils.LOGGER.debug("[DISCORD RPC] sendPresence statusChanged={} status={}", statusChanged, status);
        if (!statusChanged || status == null) return;

        // Only send presence if status has changed
        client.sendRichPresence(new RichPresence.Builder()
                .setDetails("Playing as " + MinecraftClient.getInstance().getSession().getUsername())
                .setDetailsUrl(url)
                .setState("Currently in " + status.text)
                .setStateUrl("https://eventalerts.gg")
                .setLargeImage(status.asset.get(), "Minecraft " + EventUtils.MC_VERSION, "https://eventalerts.gg")
                .setSmallImage("logo", "EventUtils " + BuildProperties.MOD_VERSION, "https://eventalerts.gg/eventutils")
                .setStartTimestamp(START)
                .build());
    }

    /**
     * @return  true if the status has changed, false if it remained the same
     */
    private boolean updateStatus() {
        final Status oldStatus = status;
        final MinecraftClient client = MinecraftClient.getInstance();
        final IntegratedServer server = client.getServer();
        if (server != null && server.isRunning()) {
            status = Status.SINGLEPLAYER;
        } else if (client.getCurrentServerEntry() != null) {
            status = Status.MULTIPLAYER;
        } else {
            status = Status.MAIN_MENU;
        }
        return oldStatus != status;
    }

    @NotNull
    private EAAction<String> retrieveUrl(@NotNull UUID uuid) {
        return mod.http.players.retrieveOneByMinecraftUuid(uuid)
                .filter(player -> player != null && player.discord != null)
                .map(player -> "https://eventalerts.gg/players/" + Objects.requireNonNull(player.discord).id)
                .onErrorMap(t -> "https://namemc.com/profile/" + uuid);
    }

    private enum Status {
        SINGLEPLAYER("Singleplayer", "dirt"),
        MULTIPLAYER("Multiplayer", () -> "https://api.mcstatus.io/v2/icon/" + Objects.requireNonNull(MinecraftClient.getInstance().getCurrentServerEntry()).address),
        MAIN_MENU("the Main Menu", "grass");

        @NotNull private final String text;
        @NotNull private final Supplier<String> asset;

        Status(@NotNull String text, @NotNull Supplier<String> asset) {
            this.text = text;
            this.asset = asset;
        }

        Status(@NotNull String text, @NotNull String asset) {
            this(text, () -> asset);
        }
    }
}
