package cc.aabss.eventutils.api.websocket;

import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cc.aabss.eventutils.EventUtils.LOGGER;

public class WebSocketEvent {
    public WebSocket webSocket;
    public final CountDownLatch latch;
    public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public WebSocketEvent(SocketEndpoint event) {
        this(event, new CountDownLatch(1));
    }

    public WebSocketEvent(SocketEndpoint event, CountDownLatch latch) {
        this.latch = latch;
        connect(event);
    }

    private void connect(SocketEndpoint event) {
        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Builder webSocketBuilder = client.newWebSocketBuilder();
        WebSocketListener listener = new WebSocketListener(event, latch, this);
        webSocketBuilder.buildAsync(
                URI.create("wss://eventalerts.venox.network/api/v1/socket/" + event.name().toLowerCase()),
                listener
        ).whenComplete((webSocket, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to establish WebSocket connection: {}", throwable.getMessage());
                retryConnection(event);
            } else {
                this.webSocket = webSocket;
                this.webSocket.request(1);
                LOGGER.info("{} WebSocket connection established", event.name());
                scheduler.scheduleAtFixedRate(() -> {
                    if (!webSocket.isInputClosed()) {
                        webSocket.sendPing(java.nio.ByteBuffer.wrap(new byte[]{1}));
                    }
                }, 0, 30, TimeUnit.SECONDS);
            }
        });
    }

    public void retryConnection(SocketEndpoint event) {
        webSocket.sendClose(1000, "EventUtils client ("+ MinecraftClient.getInstance().getSession().getUsername() +") closed");
        scheduler.schedule(() -> connect(event), 5, TimeUnit.SECONDS);
    }

    public enum SocketEndpoint {
        EVENT_POSTED,
        POTENTIAL_FAMOUS_EVENT,
        FAMOUS_EVENT,
        SERVER_ENABLED,
        SERVER_EDITED,
        BOOSTER_PASS_GIVEN
    }
}
