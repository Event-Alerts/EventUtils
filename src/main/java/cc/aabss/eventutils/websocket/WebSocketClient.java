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
    @NotNull private static final ByteBuffer PING = ByteBuffer.wrap(new byte[]{0});

    @NotNull private final EventUtils mod;
    @NotNull private final SocketEndpoint endpoint;
    @Nullable private WebSocket webSocket;
    @Nullable private ScheduledFuture<?> keepAlive;
    private boolean isRetrying = false;

    public WebSocketClient(@NotNull EventUtils mod, @NotNull SocketEndpoint endpoint) {
        this.mod = mod;
        this.endpoint = endpoint;
        connect();
    }

    private void connect() {
        final String name = endpoint.name().toLowerCase();
        try (final HttpClient client = HttpClient.newHttpClient()) {
            client.newWebSocketBuilder()
                    .buildAsync(URI.create("wss://eventalerts.venox.network/api/v1/socket/" + name), this)
                    .whenComplete((newSocket, throwable) -> {
                        if (throwable != null) {
                            EventUtils.LOGGER.error("Failed to establish WebSocket connection: {}", throwable.getMessage());
                            retryConnection("Error thrown when establishing connection");
                            return;
                        }
                        webSocket = newSocket;
                        webSocket.request(1);
                        EventUtils.LOGGER.info("{} socket connection established", name);
                        keepAlive = EventUtils.SCHEDULER.scheduleAtFixedRate(() -> {
                            if (newSocket.isInputClosed()) {
                                retryConnection("Keep-alive detected closed input");
                                return;
                            }
                            newSocket.sendPing(PING);
                        }, 0, 30, TimeUnit.SECONDS);
                    });
        }
    }

    public void retryConnection(@NotNull String reason) {
        if (isRetrying) return;
        isRetrying = true;
        EventUtils.LOGGER.warn("Retrying websocket connection for {} with reason \"{}\"", endpoint, reason);
        close("Retrying connection");
        EventUtils.SCHEDULER.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    public void close(@NotNull String reason) {
        if (webSocket != null) webSocket.sendClose(1000, reason);
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
        if (keepAlive != null) keepAlive.cancel(true);
        if (statusCode == 1006) { // Abnormal closure
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
