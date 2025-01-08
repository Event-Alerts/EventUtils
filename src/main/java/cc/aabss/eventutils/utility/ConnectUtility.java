package cc.aabss.eventutils.utility;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.MinecraftClient;
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


public class ConnectUtility {
    public static void connect(@NotNull String ip) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        final ServerInfo currentServer = client.getCurrentServerEntry();
        if (currentServer != null && currentServer.address.equalsIgnoreCase(ip)) return;

        final TitleScreen screen = new TitleScreen();
        final ServerAddress address = ServerAddress.parse(ip);
        client.execute(() -> {
            try {
                client.disconnect();

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
    public static String getIp(@NotNull String event) {
        // Get strings
        final List<String> strings = new ArrayList<>();
        for (final String string : removeMarkdown(event).split("\\s+|\\n+")) {
            if (string.contains(".") && !string.contains("/")) strings.add(string);
        }

        // Get IP
        final int size = strings.size();
        if (size == 1) {
            //? if java: <21 {
            /*return strings.get(0);
            *///?} else {
            return strings.getFirst();
            //?}
        }
        if (size > 1) for (final String string : strings) if (isValidIp(string)) return string;

        // No IP found
        return null;
    }

    private static boolean isValidIp(@NotNull String ip) {
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

    @NotNull
    public static String removeMarkdown(@NotNull String string) {
        return string
                // Remove HTML tags
                .replaceAll("<[^>]+>", "")
                // Remove setext-style headers
                .replaceAll("^[=\\-]{2,}\\s*$", "")
                // Remove footnotes
                .replaceAll("\\[\\^.+?](: .*?$)?", "")
                .replaceAll("\\s{0,2}\\[.*?]: .*?$", "")
                // Remove images
                .replaceAll("!\\[(.*?)][\\[(].*?[])]", "$1")
                // Remove inline links
                .replaceAll("\\[([^]]*?)][\\[(].*?[])]", "$2")
                // Remove blockquotes
                .replaceAll("(?m)^(\\n)?\\s{0,3}>\\s?", "$1")
                // Remove reference-style links
                .replaceAll("^\\s{1,2}\\[(.*?)]: (\\S+)( \".*?\")?\\s*$", "")
                // Remove atx-style headers
                .replaceAll("(?m)^(\\n)?\\s*#{1,6}\\s*( (.+))? +#+$|^\\s*#{1,6}\\s*( (.+))?$", "$1$3$4$5")
                // Remove * emphasis
                .replaceAll("([*]+)(\\S)(.*?\\S)??\\1", "$2$3")
                // Remove _ emphasis
                .replaceAll("(^|\\W)(_+)(\\S)(.*?\\S)??\\2($|\\W)", "$1$3$4$5")
                // Remove code blocks
                .replaceAll("(`{3,})(.*?)\\1", "$2")
                // Remove inline code
                .replaceAll("`(.+?)`", "$1")
                // Replace strike through
                .replaceAll("~(.*?)~", "$1");
    }
}