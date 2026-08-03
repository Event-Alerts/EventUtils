package cc.aabss.eventutils.utility;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.minecraft.text.Text.translatable;


public class ConnectUtility {
    public static void connect(@NotNull String ip) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        final ServerInfo currentServer = client.getCurrentServerEntry();
        if (currentServer != null && currentServer.address.equalsIgnoreCase(ip)) return;

        // Update metric
        EventUtils.MOD.stats.eventsJoined.incrementAndGet();

        final TitleScreen screen = new TitleScreen();
        final ServerAddress address = ServerAddress.parse(ip);
        client.execute(() -> {
            try {
                //? if >=1.21.11 {
                /*client.disconnect(new MessageScreen(translatable("multiplayer.disconnect.generic")), false, false);
                *///?} else if >=1.21 {
                client.disconnect(new MessageScreen(translatable("multiplayer.disconnect.generic")), false);
                //?} else {
                /*client.disconnect(new MessageScreen(translatable("multiplayer.disconnect.generic")));
                *///?}

                //? if <=1.20.4 {
                /*ConnectScreen.connect(screen, client, address, new ServerInfo("EventUtils Event Server", ip, ServerInfo.ServerType.OTHER), true);
                *///?} else {
                ConnectScreen.connect(screen, client, address, new ServerInfo("EventUtils Event Server", ip, ServerInfo.ServerType.OTHER), true, null);
                //?}
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to connect to server: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Nullable
    public static String getIp(@Nullable String message) {
        if (message == null) return null;

        // Get strings
        final List<String> strings = new ArrayList<>();
        for (final String string : MarkdownSanitizer.sanitize(message).split("\\s+|\\n+")) {
            if (string.contains(".") && !string.contains("/")) strings.add(string);
        }

        // Get IP
        final int size = strings.size();
        if (size == 1) {
            final String ip = strings.get(0); // don't use getFirst() to support lower Java versions
            return !ip.isEmpty() ? ip : null;
        }
        if (size > 1) for (final String string : strings) if (isValidIp(string)) return string;

        // No IP found
        return null;
    }

    private static boolean isValidIp(@NotNull String ip) {
        if (ip.isEmpty()) return false;

        // Get request
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder(new URI("https://api.mcstatus.io/v2/status/java/" + ip)).build();
        } catch (final URISyntaxException e) {
            return false;
        }

        // Check if valid
        final HttpClient client = HttpClient.newHttpClient();
        try {
            final String body = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get().body();
            //? if java: >=21
            client.close();
            return !body.endsWith(":null}") && !body.endsWith("Not Found") && !body.endsWith("Invalid address value");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException ignored) {}
        return false;
    }
}