package cc.aabss.eventutils.manager;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EnrichedPlayer;
import gg.eventalerts.sdk.http.action.EAAction;
import gg.eventalerts.sdk.http.exception.EAHttpResponseException;
import gg.eventalerts.sdk.http.object.body.EUOnlineAuthBody;
import gg.eventalerts.sdk.http.object.body.EUOnlineAuthUpdateBody;
import gg.eventalerts.sdk.http.object.response.EUOnlineAuthResponse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;
import xyz.srnyx.javautilities.manipulation.Mapper;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class AuthManager {
    @NotNull private static final Duration FAILED_ATTEMPT_DELAY = Duration.ofMinutes(5);
    @NotNull private static final Duration HEARTBEAT_BEFORE_EXPIRATION = Duration.ofMinutes(1);
    @NotNull private static final Duration MIN_HEARTBEAT_INTERVAL = Duration.ofMinutes(3);

    @NotNull private final EventUtils mod;
    @Nullable public EnrichedPlayer player;
    @Nullable private ScheduledFuture<?> heartbeat;

    public AuthManager(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public boolean isAuthenticated() {
        return player != null;
    }

    @NotNull @CheckReturnValue
    public EAAction<EUOnlineAuthResponse> authenticate() {
        if (heartbeat != null) heartbeat.cancel(false);

        // Get session and access token
        final User session = Minecraft.getInstance().getUser();
        final String accessToken = session.getAccessToken();
        if (accessToken.equals("FabricMC")) { // Development/offline environment
            player = null;
            return EAAction.completed(null);
        }

        // POST auth
        final EUOnlineAuthBody body = new EUOnlineAuthBody(
                session.getAccessToken(),
                session.getProfileId(),
                session.getName(),
                BuildProperties.MOD_VERSION,
                Objects.requireNonNullElse(EventUtils.MC_VERSION, "unknown"));
        return mod.http.players.eventUtils.postOnlineAuth(body)
                .onSuccess(response -> {
                    player = response.player != null ? new EnrichedPlayer(response.player) : null;
                    if (response.onlinePlayers != null) mod.cacheManager.players().addToCache(response.onlinePlayers);
                    mod.http.setEventUtilsKey(response.token);
                    EventUtils.LOGGER.debug("[AUTH] Authenticated with Event Alerts response={}", response);

                    // Schedule first heartbeat (default to minimum if null)
                    scheduleHeartbeat(response.heartbeatExpiresAt != null
                            ? response.heartbeatExpiresAt.getTime() - System.currentTimeMillis() - HEARTBEAT_BEFORE_EXPIRATION.toMillis()
                            : MIN_HEARTBEAT_INTERVAL.toMillis());
                })
                .onError(t -> {
                    player = null;
                    EventUtils.LOGGER.error("[AUTH] Failed to authenticate with Event Alerts", t);

                    // Schedule retry
                    MiscUtility.IO_SCHEDULER.schedule(() -> authenticate().queue(), FAILED_ATTEMPT_DELAY.toMillis(), TimeUnit.MILLISECONDS);
                });
    }

    private void scheduleHeartbeat(long intervalMillis) {
        // Ensure minimum interval
        if (intervalMillis < MIN_HEARTBEAT_INTERVAL.toMillis()) intervalMillis = MIN_HEARTBEAT_INTERVAL.toMillis();
        EventUtils.LOGGER.debug("[AUTH] Scheduling heartbeat in {}ms", intervalMillis);

        // Schedule
        if (heartbeat != null) heartbeat.cancel(false);
        heartbeat = MiscUtility.IO_SCHEDULER.schedule(() -> {
            // Not authenticated (shouldn't happen)
            if (!isAuthenticated()) {
                EventUtils.LOGGER.warn("[AUTH] Heartbeat failed, reauthenticating");
                authenticate().queue();
                return;
            }

            // POST heartbeat
            EventUtils.LOGGER.debug("[AUTH] Sending heartbeat");
            mod.http.players.eventUtils.postOnlineUpdate(new EUOnlineAuthUpdateBody(true)).queue(
                    response -> {
                        // Marked offline, end heartbeat
                        if (response.heartbeatExpiresAt == null) return;

                        // Update cached players
                        if (response.onlinePlayers != null) mod.cacheManager.players().addToCache(response.onlinePlayers);

                        // Schedule next heartbeat
                        scheduleHeartbeat(response.heartbeatExpiresAt.getTime() - System.currentTimeMillis() - HEARTBEAT_BEFORE_EXPIRATION.toMillis());
                    },
                    t -> {
                        // Token expired, re-authenticate
                        if (t instanceof EAHttpResponseException e && e.getResponseBodyJsonOptional()
                                .flatMap(object -> Mapper.convertJsonElementToPrimitive(object.get("expired"), Boolean.class))
                                .orElse(false)) {
                            EventUtils.LOGGER.warn("[AUTH] Token expired, reauthenticating");
                            authenticate().queue();
                            return;
                        }

                        // Unknown error
                        EventUtils.LOGGER.error("[AUTH] Failed to send heartbeat to Event Alerts", t);
                    });
        }, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        // Stop heartbeat
        if (heartbeat != null) heartbeat.cancel(false);
        heartbeat = null;

        // Mark as offline with Event Alerts
        if (isAuthenticated()) mod.http.players.eventUtils.postOnlineUpdate(new EUOnlineAuthUpdateBody(false)).queue(
                response -> EventUtils.LOGGER.debug("[AUTH] Marked offline"),
                t -> EventUtils.LOGGER.error("[AUTH] Failed to mark offline", t));
    }
}
