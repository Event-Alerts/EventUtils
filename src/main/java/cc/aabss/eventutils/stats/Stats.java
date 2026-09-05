package cc.aabss.eventutils.stats;

import cc.aabss.eventutils.BuildProperties;
import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.stats.gson.StatsGson;
import com.google.gson.JsonObject;
import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.data.SourceId;
import dev.faststats.fabric.FabricContext;
import eu.okaeri.configs.OkaeriConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class Stats {
    @NotNull public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();

    @NotNull private final AtomicLong socketMessagesReceivedPrevious = new AtomicLong();
    @NotNull private final AtomicLong socketMessagesSentPrevious = new AtomicLong();
    @NotNull public final AtomicInteger eventToastsReceived = new AtomicInteger();
    @NotNull public final AtomicInteger eventsJoined = new AtomicInteger();
    @NotNull public final AtomicInteger eventsPosted = new AtomicInteger();

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
                        eventsPosted.set(0);
                    });

                    // Config
                    factory.addMetric(config("config", () -> mod.config));

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
                    factory.addMetric(Metric.number("events_posted", eventsPosted::get));

                    return factory.create();
                })
                .create();
    }

    @NotNull
    public static Metric<JsonObject> config(@NotNull @SourceId String id, @NotNull Callable<@Nullable OkaeriConfig> callable) {
        return Metric.object(id, () -> {
            final OkaeriConfig config = callable.call();
            return config == null ? null : StatsGson.GSON.toJsonTree(config, OkaeriConfig.class).getAsJsonObject();
        });
    }
}
