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
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class DiscordRPC {
    @NotNull private final EventUtils mod;
    @Nullable public DiscordRPCClient client;

    public DiscordRPC(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void connect() {
        if (!mod.config.discordRpc || client != null) return;

        final String username = MinecraftClient.getInstance().getSession().getUsername();
        final long start = System.currentTimeMillis() / 1000;
        client = new DiscordRPCClient(new EventListener() {
            @Override
            public void onReady(@NotNull DiscordRPCClient newClient, @NotNull User user) {
                EventUtils.LOGGER.info("[DISCORD] Logged in as {}#{}", user.username, user.discriminator);
                EventUtils.SCHEDULER.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        final Status status = Status.get();
                        newClient.sendPresence(new RichPresence.Builder()
                                .addButton("Minecraft Mod", "https://modrinth.com/mod/alerts")
                                .addButton("Discord Server", "https://discord.gg/uFPFNYzAWC")
                                .setTimestamps(start, null)
                                .setText("Playing as " + username, "Currently in " + status.text)
                                .setAssets(status.asset.get(), "Minecraft v" + (Versions.MC_VERSION != null ? Versions.MC_VERSION : "???"), "event_alerts", "EventUtils v" + (Versions.EU_VERSION != null ? Versions.EU_VERSION : "???"))
                                .build());
                    }
                }, 0, 5, TimeUnit.SECONDS);
            }
        }, "1236917260036083743");

        try {
            client.connect();
        } catch (final DiscordException ignored) {
            client = null;
        }
    }

    public void disconnect() {
        if (client == null) return;
        if (client.isConnected) client.disconnect();
        client = null;
    }

    private enum Status {
        SINGLEPLAYER("Singleplayer", "singleplayer"),
        MULTIPLAYER("Multiplayer", () -> "https://api.mcstatus.io/v2/icon/" + Objects.requireNonNull(MinecraftClient.getInstance().getCurrentServerEntry()).address),
        MAIN_MENU("the Main Menu", "themainmenu");

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
