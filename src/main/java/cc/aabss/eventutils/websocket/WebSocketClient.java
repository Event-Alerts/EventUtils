package cc.aabss.eventutils.websocket;

import cc.aabss.eventutils.EventUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class WebSocketClient implements WebSocket.Listener {
    @NotNull private static final String HOST = "eventalerts.gg";
    @NotNull private static final ByteBuffer PING = ByteBuffer.wrap(new byte[]{0});

    @NotNull private final EventUtils mod;
    @NotNull private final SocketEndpoint endpoint;
    @Nullable private WebSocket webSocket;
    @Nullable private HttpClient httpClient; // Do we even need to store/close this?
    @Nullable private ScheduledFuture<?> keepAlive;
    private boolean isRetrying = false;

    public WebSocketClient(@NotNull EventUtils mod, @NotNull SocketEndpoint endpoint) {
        this.mod = mod;
        this.endpoint = endpoint;
        connect();
    }

    private void connect() {
        EventUtils.LOGGER.info("Attempting to establish WebSocket connection for {}", endpoint);
        httpClient = HttpClient.newHttpClient();
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create("wss://" + HOST + "/api/v1/socket/" + endpoint.name().toLowerCase()), this)
                .whenComplete((newSocket, throwable) -> {
                    isRetrying = false;
                    if (throwable != null) {
                        EventUtils.LOGGER.error("Failed to establish WebSocket connection!", throwable);
                        retryConnection("Error thrown when establishing connection");
                        return;
                    }
                    webSocket = newSocket;
                    webSocket.request(1);
                    keepAlive = EventUtils.SCHEDULER.scheduleAtFixedRate(() -> {
                        if (newSocket.isInputClosed()) {
                            retryConnection("Keep-alive detected closed input");
                            return;
                        }
                        newSocket.sendPing(PING);
                    }, 0, 30, TimeUnit.SECONDS);
                    EventUtils.LOGGER.info("{} socket connection established", endpoint);
                });
    }

    public void retryConnection(@NotNull String reason) {
        if (isRetrying) return;
        isRetrying = true;
        close("Retrying connection");
        EventUtils.SCHEDULER.schedule(() -> {
            EventUtils.LOGGER.warn("Retrying websocket connection for {} with reason \"{}\"", endpoint, reason);
            connect();
        }, 10, TimeUnit.SECONDS);
    }

    public void close(@NotNull String reason) {
        if (webSocket != null) webSocket.sendClose(1000, reason);
        closeTasks();
    }

    private void closeTasks() {
        //? if java: >=21
        if (httpClient != null) httpClient.close();
        if (keepAlive != null) keepAlive.cancel(true);
    }

    @Override
    public CompletionStage<?> onText(@NotNull WebSocket webSocket, @NotNull CharSequence data, boolean last) {
        final String message = data.toString();
        webSocket.request(1); // Request more messages
        endpoint.handler.accept(mod, message);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(@NotNull WebSocket webSocket, int statusCode, @NotNull String reason) {
        closeTasks();
        if (statusCode == 1006) {
            retryConnection("Experienced abnormal closure");
            return null;
        }
        EventUtils.LOGGER.info("{} socket closed with status code {} and reason \"{}\"", endpoint.name(), statusCode, reason);
        return null;
    }

    @Override
    public void onError(@NotNull WebSocket webSocket, @NotNull Throwable error) {
        retryConnection("Experienced an error! See below for details");
        error.printStackTrace();
    }
}
