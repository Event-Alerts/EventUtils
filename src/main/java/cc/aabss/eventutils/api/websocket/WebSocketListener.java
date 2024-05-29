package cc.aabss.eventutils.api.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import static cc.aabss.eventutils.EventUtils.LOGGER;
import static cc.aabss.eventutils.config.EventUtil.*;

public class WebSocketListener implements WebSocket.Listener {

    private static final EventListener eventListenerInstance = new EventListener();
    private final WebSocketEvent.SocketEndpoint endpoint;
    private final CountDownLatch latch;
    private final WebSocketEvent webSocketEvent;

    public WebSocketListener(WebSocketEvent.SocketEndpoint endpoint, CountDownLatch latch, WebSocketEvent webSocketEvent) {
        this.endpoint = endpoint;
        this.latch = latch;
        this.webSocketEvent = webSocketEvent;
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();
        webSocket.request(1); // Request more messages
        handleEvent(message);
        LOGGER.info(message);
        return null;
    }

    private void handleEvent(String message) {
        try {
            JsonObject jsonmsg = JsonParser.parseString(message).getAsJsonObject();
            switch (endpoint) {
                case EVENT_POSTED:
                    if (isMoneyEvent(jsonmsg)) {
                        eventListenerInstance.onMoneyEvent(jsonmsg);
                    }
                    if (isFunEvent(jsonmsg)) {
                        eventListenerInstance.onFunEvent(jsonmsg);
                    }
                    if (isHousingEvent(jsonmsg)) {
                        eventListenerInstance.onHousingEvent();
                    }
                    if (isCivilizationEvent(jsonmsg)) {
                        eventListenerInstance.onCivilizationEvent(jsonmsg);
                    }
                    if (isPartnerEvent(jsonmsg)) {
                        eventListenerInstance.onPartnerEvent(jsonmsg);
                    }
                    if (isCommunityEvent(jsonmsg)){
                        eventListenerInstance.onCommunityEvent(jsonmsg);
                    }
                    break;
                case FAMOUS_EVENT:
                    eventListenerInstance.onFamousEvent(message);
                    break;
                case POTENTIAL_FAMOUS_EVENT:
                    eventListenerInstance.onPotentialFamousEvent(message);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (statusCode == 1006) { // Abnormal closure
            LOGGER.info("Reconnecting due to abnormal closure...");
            webSocketEvent.retryConnection(endpoint);
        } else{
            LOGGER.warn("{} WEBSOCKET CLOSED | STATUS: {} | REASON: {}", endpoint.name(), statusCode, reason);
            latch.countDown();
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOGGER.warn("WEBSOCKET ERROR: {}", error.getMessage());
        webSocketEvent.retryConnection(endpoint);
        latch.countDown();
        throw new RuntimeException(error);
    }
}
