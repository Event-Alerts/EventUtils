package cc.aabss.eventutils;

import club.bottomservices.discordrpc.lib.DiscordRPCClient;
import club.bottomservices.discordrpc.lib.EventListener;
import club.bottomservices.discordrpc.lib.RichPresence;
import club.bottomservices.discordrpc.lib.User;
import club.bottomservices.discordrpc.lib.exceptions.DiscordException;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class DiscordRPC {
    private static final long START = System.currentTimeMillis() / 1000;

    @NotNull private final EventUtils mod;
    @Nullable public DiscordRPCClient client;

    public DiscordRPC(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void connect() {
        if (!mod.config.discordRpc || client != null) return;

        // Build presence
        final String username = MinecraftClient.getInstance().getSession().getUsername();
        client = new DiscordRPCClient(new EventListener() {
            @Override
            public void onReady(@NotNull DiscordRPCClient newClient, @NotNull User user) {
                EventUtils.LOGGER.info("DISCORD RPC: Logged in as {}", user.username);
                mod.scheduler.scheduleAtFixedRate(() -> {
                    final Status status = Status.get();
                    newClient.sendPresence(new RichPresence.Builder()
                            .setText("Playing as " + username, "Currently in " + status.text)
                            .setTimestamps(START, null)
                            .setAssets(status.asset.get(), "Minecraft" + (Versions.MC_VERSION != null ? " v" + Versions.MC_VERSION : ""), "logo", "EventUtils" + (Versions.EU_VERSION != null ? " v" + Versions.EU_VERSION : ""))
                            .addButton("Download the mod!", "https://modrinth.com/mod/alerts")
                            .addButton("Join the Discord!", "https://discord.gg/aGDuQcduWZ")
                            .build());
                }, 0, 10, TimeUnit.SECONDS);
            }
        }, "1351016544779374735");

        // Connect
        try {
            client.connect();
        } catch (final DiscordException e) {
            EventUtils.LOGGER.error("DISCORD RPC: Failed to connect", e);
            client = null;
        }
    }

    public void disconnect() {
        if (client == null) return;
        if (client.isConnected) client.disconnect();
        client = null;
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

        @NotNull
        public static Status get() {
            final MinecraftClient client = MinecraftClient.getInstance();
            final IntegratedServer server = client.getServer();
            if (server != null && server.isRunning()) return Status.SINGLEPLAYER;
            if (client.getCurrentServerEntry() != null) return Status.MULTIPLAYER;
            return Status.MAIN_MENU;
        }
    }
}
