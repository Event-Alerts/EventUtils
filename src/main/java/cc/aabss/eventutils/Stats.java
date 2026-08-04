package cc.aabss.eventutils;

import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.fabric.FabricContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class Stats {
    @NotNull public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();

    @NotNull private final AtomicLong socketMessagesReceivedPrevious = new AtomicLong();
    @NotNull private final AtomicLong socketMessagesSentPrevious = new AtomicLong();
    @NotNull public final AtomicInteger eventToastsReceived = new AtomicInteger();
    @NotNull public final AtomicInteger eventsJoined = new AtomicInteger();

    public Stats(@NotNull EventUtils mod) {
        new FabricContext.Factory(BuildProperties.MOD_ID, "5de470ff674a5d47edfd04ad3f99e230")
                .errorTrackerService(ERROR_TRACKER)
                .metrics(factory -> {
                    // FLUSH
                    factory.onFlush(() -> {
                        socketMessagesReceivedPrevious.set(mod.webSocket.messagesReceived);
                        socketMessagesSentPrevious.set(mod.webSocket.messagesSent);
                        eventToastsReceived.set(0);
                        eventsJoined.set(0);
                    });

                    // Config TODO: take from annoying api FastStatsLoader.config(...) and StatsGson (adapters too)
//                    factory.addMetric(Metric.object("config", mod.config::getJson));

                    // WebSocket
                    factory.addMetric(Metric.bool("socket_open", () -> mod.webSocket.isOpen()));
                    factory.addMetric(Metric.number("socket_connected_at", () -> mod.webSocket.connectedAt != null ? mod.webSocket.connectedAt.getTime() : null));
                    factory.addMetric(Metric.number("socket_messages_received", () -> mod.webSocket.messagesReceived - socketMessagesReceivedPrevious.get()));
                    factory.addMetric(Metric.number("socket_messages_sent", () -> mod.webSocket.messagesSent - socketMessagesSentPrevious.get()));
                    factory.addMetric(Metric.number("socket_last_message_received_at", () -> mod.webSocket.lastMessageReceivedAt != null ? mod.webSocket.lastMessageReceivedAt.getTime() : null));
                    factory.addMetric(Metric.number("socket_last_message_sent_at", () -> mod.webSocket.lastMessageSentAt != null ? mod.webSocket.lastMessageSentAt.getTime() : null));

                    // Misc
                    factory.addMetric(Metric.number("event_toasts_received", eventToastsReceived::get));
                    factory.addMetric(Metric.number("events_joined", eventsJoined::get));

                    return factory.create();
                })
                .create();
    }
}
