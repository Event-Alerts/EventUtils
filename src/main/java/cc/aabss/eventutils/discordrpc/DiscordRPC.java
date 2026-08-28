package cc.aabss.eventutils.discordrpc;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.EventUtils;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import gg.eventalerts.sdk.http.action.EAAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;

import java.time.Duration;
import java.util.List;
import java.util.Objects;


public class DiscordRPC {
    @NotNull public static final Duration REFRESH_INTERVAL = Duration.ofSeconds(30);
    @NotNull private static final List<String> SINGLEPLAYER_TEXTS = List.of(
            "Punching trees 🌳", "Looking for diamonds 💎", "Digging straight down", "Definitely not cheating",
            "Just one more block...", "Escaping creepers", "Losing track of time", "Causing villager inflation",
            "Speedrunning disappointment", "Organizing chests (again)", "Getting lost underground", "Building questionable houses",
            "Hoarding cobblestone", "Chasing achievements", "Taming the wilderness", "Living the block life", "Touching grass blocks",
            "Trying not to fall in lava", "Fighting the urge to dig down", "Defying Minecraft logic", "Respawning soon™", "Punch first, ask later",
            "Crafting bad decisions", "Collecting shiny rocks", "Searching for the perfect spot", "Pretending this is temporary",
            "I'll stop after this project", "Accidentally starting a megabase", "Making the world slightly weirder", "Convincing pigs to cooperate",
            "One block at a time", "Busy being square", "Breaking and placing blocks", "Leaving floating trees behind 🌲", "Mining \"responsibly\"");

    private static final long START = System.currentTimeMillis();

    @NotNull private final EventUtils mod;
    @NotNull private final IPCClient client = new IPCClient(1351016544779374735L);
    @Nullable private Status status;
    @Nullable private Presence presence;
    @NotNull private final String playerUrl;

    public DiscordRPC(@NotNull EventUtils mod) {
        this.mod = mod;
        this.client.setListener(new CustomIPCListener(this));
        this.playerUrl = mod.authManager.player != null && mod.authManager.player.player.discord != null && mod.authManager.player.player.discord.id != null
                ? "https://eventalerts.gg/players/" + mod.authManager.player.player.discord.id
                : "https://namemc.com/profile/" + Minecraft.getInstance().getUser().getProfileId();
        refreshConnection();
    }

    public void refreshConnection() {
        final PipeStatus status = client.getStatus();
        EventUtils.LOGGER.debug("[DISCORD RPC] config.discordRpc={} status={}", mod.config.discord_rpc, status);

        // Enable
        if (mod.config.discord_rpc) {
            // Already enabled
            if (status == PipeStatus.CONNECTING || status == PipeStatus.CONNECTED) return;

            // Enable
            try {
                EventUtils.LOGGER.debug("[DISCORD RPC] Connecting...");
                client.connect();
                EventUtils.LOGGER.debug("[DISCORD RPC] Connected");
            } catch (final Exception e) {
                if (!(e instanceof NoDiscordClientException)) EventUtils.LOGGER.warn("[DISCORD RPC] Failed to connect!", e);
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

    public void refresh() {
        getNewPresence().queue(newPresence -> {
            if (Objects.equals(presence, newPresence)) return;
            presence = newPresence;

            // Build presence
            final RichPresence.Builder builder = new RichPresence.Builder().setStartTimestamp(START);
            presence.apply(builder);

            // Send presence
            client.sendRichPresence(builder.build());
        });
    }

    @NotNull
    private EAAction<Presence> getNewPresence() {
        final Presence newPresence = new Presence();

        final Minecraft client = Minecraft.getInstance();
        final IntegratedServer clientServer = client.getSingleplayerServer();
        final ServerData multiplayerServer = client.getCurrentServer();

        if (clientServer != null && clientServer.isRunning()) {
            // Singleplayer
            if (status == Status.SINGLEPLAYER) return EAAction.completed(presence);
            status = Status.SINGLEPLAYER;
            newPresence.details(SINGLEPLAYER_TEXTS.get(MiscUtility.RANDOM.nextInt(SINGLEPLAYER_TEXTS.size())));

        } else if (multiplayerServer != null) {
            // Multiplayer + Event
            newPresence
                    .largeImage("https://api.mcstatus.io/v2/icon/" + multiplayerServer.ip)
                    .largeImageUrl("https://eventalerts.gg");

            if (mod.inEvent != null) {
                // Event
                if (status == Status.EVENT) return EAAction.completed(presence);
                status = Status.EVENT;

                // Event info (details)
                final String eventUrl = "https://eventalerts.gg/events/" + (mod.inEvent.event.id != null ? mod.inEvent.event.id : "");
                newPresence
                        .details("Playing in " + Objects.requireNonNullElse(mod.inEvent.event.title, "an event"))
                        .detailsUrl(eventUrl)
                        .largeImageUrl(eventUrl);

                // Host info (state)
                EventUtils.LOGGER.debug("[DISCORD RPC] Retrieving host event={}", mod.inEvent.event);
                return mod.inEvent.getHost()
                        .onSuccess(host -> {
                            if (host == null) return;
                            EventUtils.LOGGER.debug("[DISCORD RPC] host={}", host);

                            // Minecraft username -> Discord username
                            String hostName = host.minecraft != null ? host.minecraft.username : null;
                            if (hostName == null && host.discord != null) hostName = host.discord.username;

                            // EA players URL (Discord ID) -> NameMC Minecraft UUID -> event URL
                            String hostUrl = host.discord != null && host.discord.id != null ? "https://eventalerts.gg/players/" + host.discord.id : null;
                            if (hostUrl == null && host.minecraft != null && host.minecraft.uuid != null) hostUrl = "https://namemc.com/profile/" + host.minecraft.uuid;
                            if (hostUrl == null) hostUrl = eventUrl;

                            // Only set state if host name found
                            EventUtils.LOGGER.debug("[DISCORD RPC] hostName={} hostUrl={}", hostName, hostUrl);
                            if (hostName != null) newPresence
                                    .state("Hosted by " + hostName)
                                    .stateUrl(hostUrl);
                        })
                        .map(ignored -> newPresence);
            } else {
                // Multiplayer
                if (status == Status.MULTIPLAYER) return EAAction.completed(presence);
                status = Status.MULTIPLAYER;
                newPresence.details("Playing on " + multiplayerServer.name);
            }

        } else {
            // Other
            if (status == Status.OTHER) return EAAction.completed(presence);
            status = Status.OTHER;
        }

        return EAAction.completed(newPresence);
    }

    private enum Status {
        SINGLEPLAYER,
        MULTIPLAYER,
        EVENT,
        OTHER
    }

    /**
     * Much easier to use and store than {@link RichPresence.Builder} (doesn't allow only modifying image texts/URLs)
     */
    private class Presence {
        @NotNull public String details = "Waiting for an event...";
        @NotNull public String detailsUrl = "https://eventalerts.gg";
        @NotNull public String state;
        @NotNull public String stateUrl = playerUrl;
        @NotNull public String largeImage;
        @NotNull public String largeImageText = "Minecraft " + EventUtils.MC_VERSION;
        @NotNull public String largeImageUrl = stateUrl;
        @NotNull public String smallImage = "logo";
        @NotNull public String smallImageText = "EventUtils " + BuildProperties.MOD_VERSION;
        @NotNull public String smallImageUrl = "https://eventalerts.gg/eventutils";

        public Presence() {
            final User session = Minecraft.getInstance().getUser();
            this.state = "Playing as " + session.getName();
            this.largeImage = "https://mc-heads.net/avatar/" + session.getProfileId();
        }

        public void apply(@NotNull RichPresence.Builder builder) {
            builder
                    .setDetails(details)
                    .setDetailsUrl(detailsUrl)
                    .setState(state)
                    .setStateUrl(stateUrl)
                    .setLargeImage(largeImage, largeImageText, largeImageUrl)
                    .setSmallImage(smallImage, smallImageText, smallImageUrl);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if ((!(o instanceof Presence other))) return false;
            return details.equals(other.details) && detailsUrl.equals(other.detailsUrl)
                    && state.equals(other.state) && stateUrl.equals(other.stateUrl)
                    && largeImage.equals(other.largeImage) && largeImageText.equals(other.largeImageText) && largeImageUrl.equals(other.largeImageUrl)
                    && smallImage.equals(other.smallImage) && smallImageText.equals(other.smallImageText) && smallImageUrl.equals(other.smallImageUrl);
        }

        @NotNull
        public DiscordRPC.Presence details(@NotNull String details) {
            this.details = details;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence detailsUrl(@NotNull String detailsUrl) {
            this.detailsUrl = detailsUrl;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence state(@NotNull String state) {
            this.state = state;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence stateUrl(@NotNull String stateUrl) {
            this.stateUrl = stateUrl;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence largeImage(@NotNull String largeImage) {
            this.largeImage = largeImage;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence largeImageText(@NotNull String largeImageText) {
            this.largeImageText = largeImageText;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence largeImageUrl(@NotNull String largeImageUrl) {
            this.largeImageUrl = largeImageUrl;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence smallImage(@NotNull String smallImage) {
            this.smallImage = smallImage;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence smallImageText(@NotNull String smallImageText) {
            this.smallImageText = smallImageText;
            return this;
        }

        @NotNull
        public DiscordRPC.Presence smallImageUrl(@NotNull String smallImageUrl) {
            this.smallImageUrl = smallImageUrl;
            return this;
        }
    }
}
