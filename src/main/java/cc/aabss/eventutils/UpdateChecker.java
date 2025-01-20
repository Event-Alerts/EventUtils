package cc.aabss.eventutils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static net.minecraft.text.Text.translatable;


public class UpdateChecker {
    private void notifyUpdate(@NotNull String latestVersion) {
        final MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> {
            if (client.player == null) return;
            client.player.sendMessage(Text.literal("§6[EVENTUTILS]§r §e" + EventUtils.translate("eventutils.updatechecker.new")+"§r §7(v" + Versions.EU_VERSION + " -> v" + latestVersion.replace(Versions.MC_VERSION + "-", "") + ")" + "\n")
                            .setStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translatable("eventutils.updatechecker.hover")))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/alerts/version/" + latestVersion)))
                            .append(Text.literal("§7§o" + EventUtils.translate("eventutils.updatechecker.config"))
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eventutils config")))),
                    false);
        });
    }

    public void checkUpdate() {
        if (!EventUtils.MOD.config.updateChecker || Versions.MC_VERSION == null || Versions.EU_VERSION == null || Versions.EU_VERSION_SEMANTIC == null) return;

        // Ensure client in-game
        if (MinecraftClient.getInstance().player == null) return;

        final HttpClient httpClient = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.modrinth.com/v2/project/alerts/version?game_versions=%5B%22" + Versions.MC_VERSION + "%22%5D"))
                    .header("User-Agent", "EventUtils/" + Versions.EU_VERSION + " (Minecraft/" + Versions.MC_VERSION + ")")
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> {
                        try {
                            JsonObject latestVersionObj = JsonParser.parseString(body)
                                    .getAsJsonArray()
                                    .get(0)
                                    .getAsJsonObject();

                            if (latestVersionObj != null && latestVersionObj.has("version_number")) {
                                final String latestVersion = latestVersionObj.get("version_number").getAsString();
                                final String currentVersion = Versions.MC_VERSION + "-" + Versions.EU_VERSION;

                                if (!currentVersion.equals(latestVersion)) {
                                    notifyUpdate(latestVersion);
                                }
                            } else {
                                EventUtils.LOGGER.error("Failed to check for updates: Unexpected response from Modrinth");
                            }
                        } catch (Exception e) {
                            EventUtils.LOGGER.error("Failed to parse update check:", e);
                        }
                    })
                    .exceptionally(e -> {
                        EventUtils.LOGGER.error("Failed to check for updates", e);
                        return null;
                    });
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to check for updates", e);
        }
    }
}
