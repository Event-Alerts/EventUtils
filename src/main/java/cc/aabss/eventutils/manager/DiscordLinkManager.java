package cc.aabss.eventutils.manager;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.NotificationToast;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gg.eventalerts.sdk.http.object.body.EUDiscordLinkBody;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MiscUtility;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Drives the Discord account linking flow: opens a one-shot loopback HTTP server, asks Event Alerts to build the OAuth
 * URL, opens the browser, then re-authenticates once Event Alerts redirects back to the local server
 */
public class DiscordLinkManager {
    @NotNull private static final String CALLBACK_PATH = "/eventutils/callback/link/discord";
    @NotNull private static final Duration TIMEOUT = Duration.ofMinutes(5);
    @NotNull private static final SecureRandom RANDOM = new SecureRandom();

    @NotNull private final EventUtils mod;

    @Nullable private HttpServer server;
    @Nullable private String nonce;
    @Nullable private ScheduledFuture<?> timeout;
    @Nullable private Runnable onLinked;

    public DiscordLinkManager(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    /**
     * Begins the linking flow. Safe to call from the render thread (button action)
     *
     * @param   browserParent   screen to return to after the browser-confirmation prompt
     * @param   onLinked        run on the render thread after a successful link + re-auth (e.g. to refresh the config screen)
     */
    public synchronized void startLink(@Nullable Screen browserParent, @Nullable Runnable onLinked) {
        cancel(); // Clear any previous in-progress attempt
        this.onLinked = onLinked;

        // Start the local callback server
        final int port;
        try {
            port = openServer();
        } catch (final Exception e) {
            EventUtils.LOGGER.error("[DISCORD_LINK] Failed to start local callback server", e);
            toast("eventutils.config.discord_link.toast.failed", "eventutils.config.discord_link.toast.port");
            return;
        }

        // Generate CSRF nonce
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        final String currentNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        nonce = currentNonce;

        // Schedule timeout
        timeout = MiscUtility.IO_SCHEDULER.schedule(this::timedOut, TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        // Ask Event Alerts to build the OAuth URL, then open the browser
        mod.http.players.eventUtils.postLinkDiscord(new EUDiscordLinkBody(currentNonce, port)).queue(
                response -> {
                    final String url = response != null ? response.url : null;
                    if (url == null) {
                        EventUtils.LOGGER.error("[DISCORD_LINK] Link start response missing 'url'");
                        fail("eventutils.config.discord_link.toast.start");
                        return;
                    }
                    // It's okay for browserParent to be null
                    Minecraft.getInstance().execute(() -> ConfirmLinkScreen.confirmLinkNow(browserParent, url));
                },
                t -> {
                    EventUtils.LOGGER.error("[DISCORD_LINK] Failed to request OAuth URL", t);
                    fail("eventutils.config.discord_link.toast.start");
                });
    }

    /**
     * Stops the callback server and clears all in-progress state
     */
    public synchronized void cancel() {
        stopServer();
        onLinked = null;
    }

    /**
     * Stops the callback server and its timeout, but keeps {@link #onLinked} so a successful callback can still run it
     */
    private synchronized void stopServer() {
        if (timeout != null) {
            timeout.cancel(false);
            timeout = null;
        }
        if (server != null) {
            server.stop(0);
            server = null;
        }
        nonce = null;
    }

    private int openServer() throws Exception {
        final HttpServer created = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        created.createContext(CALLBACK_PATH, this::handleCallback);
        created.createContext("/", exchange -> respond(exchange, 404, "text/plain", "Not found"));
        created.setExecutor(null);
        created.start();
        server = created;
        final int port = created.getAddress().getPort();
        EventUtils.LOGGER.debug("[DISCORD_LINK] Callback server listening on 127.0.0.1:{}", port);
        return port;
    }

    private void handleCallback(@NotNull HttpExchange exchange) {
        // Get query parameters
        final Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        final String status = params.get("status");
        final String error = params.get("error");
        final String state = params.get("state");

        // Ensure callback is valid
        final boolean success =
                // status = ok
                status != null && status.equals("ok")
                // No error
                && error == null
                // nonce = state
                && nonce != null && state != null && MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8), nonce.getBytes(StandardCharsets.UTF_8));
        if (success) {
            EventUtils.LOGGER.debug("[DISCORD_LINK] Callback received with valid state");
        } else {
            EventUtils.LOGGER.warn("[DISCORD_LINK] Callback rejected (error={}, hasState={})", error, state != null);
        }

        try {
            respond(exchange, success ? 200 : 400, "text/html", page(success, error));
        } finally {
            // Tear down and finish up off handler thread
            MiscUtility.IO_SCHEDULER.execute(() -> finish(success, error));
        }
    }

    private void finish(boolean linked, @Nullable String error) {
        stopServer();

        if (!linked) {
            fail("eventutils.config.discord_link.toast." + Objects.requireNonNullElse(error, "denied"));
            return;
        }

        EventUtils.LOGGER.debug("[DISCORD_LINK] Link callback received, re-authenticating");
        mod.authManager.authenticate().queue(
                response -> Minecraft.getInstance().execute(() -> {
                    toast("eventutils.config.discord_link.toast.linked", null);
                    final Runnable refresh = onLinked;
                    onLinked = null;
                    if (refresh != null) refresh.run();
                }),
                t -> {
                    EventUtils.LOGGER.error("[DISCORD_LINK] Re-auth after link failed", t);
                    fail("eventutils.config.discord_link.toast.reauth");
                });
    }

    private synchronized void timedOut() {
        if (server == null) return;
        EventUtils.LOGGER.warn("[DISCORD_LINK] Linking timed out");
        cancel();
    }

    private void fail(@NotNull String descriptionKey) {
        toast("eventutils.config.discord_link.toast.failed", descriptionKey);
    }

    private void toast(@NotNull String titleKey, @Nullable String descriptionKey) {
        Minecraft.getInstance().execute(() -> NotificationToast.show(Component.translatable(titleKey), descriptionKey != null ? Component.translatable(descriptionKey) : null));
    }

    @NotNull
    private static Map<String, String> parseQuery(@Nullable String query) {
        final HashMap<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (final String pair : query.split("&")) {
            final int equals = pair.indexOf('=');
            if (equals >= 0) map.put(
                    URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    private static void respond(@NotNull HttpExchange exchange, int status, @NotNull String contentType, @NotNull String body) {
        try (exchange) {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (final OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (final Exception e) {
            EventUtils.LOGGER.debug("[DISCORD_LINK] Failed to write response", e);
        }
    }

    @NotNull
    private static String page(boolean ok, @Nullable String error) {
        final String titleColor = ok ? "#43b581" : "#f04747";

        String message;
        if (ok) {
            message = "Your Discord account is linked. You can close this tab and return to Minecraft.";
        } else {
            message = "Linking failed or was cancelled. You can close this tab and try again in Minecraft.";
            if (error != null) message += "<br><br>ERROR: " + EventUtils.translate("eventutils.config.discord_link.toast." + error);
        }

        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>EventUtils</title></head>"
                + "<body style=\"font-family:sans-serif;background:#1e1f22;color:" + titleColor + ";display:flex;align-items:center;"
                + "justify-content:center;height:100vh;margin:0;text-align:center\"><div><h2>EventUtils</h2><p>"
                + message + "</p></div></body></html>";
    }
}
