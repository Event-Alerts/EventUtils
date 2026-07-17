package cc.aabss.eventutils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static net.minecraft.text.Text.translatable;


public class UpdateChecker {
    @NotNull private final EventUtils mod;

    public UpdateChecker(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    private void notifyUpdate(@NotNull String latestVersion) {
        final MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> {
            if (client.player == null) return;
            final MutableText hover = translatable("eventutils.updatechecker.hover");
            final String link = "https://modrinth.com/mod/alerts/version/" + latestVersion;
            final String configCommand = "/eventutils config";
            client.player.sendMessage(
                    EventUtils.MESSAGE_PREFIX.copy().append(" §e" + EventUtils.translate("eventutils.updatechecker.new") + "§r §7(" + BuildProperties.MOD_VERSION + " -> v" + latestVersion.replace(EventUtils.MC_VERSION + "-", "") + ")" + "\n")
                            .setStyle(EventUtils.MESSAGE_PREFIX.getStyle()
                                    //? if >=1.21.5 {
                                    /*.withHoverEvent(new HoverEvent.ShowText(hover))
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(link))))
                                    *///?} else {
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link)))
                                    //?}
                            .append(Text.literal("§7§o" + EventUtils.translate("eventutils.updatechecker.config"))
                                    //? if >=1.21.5 {
                                    /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(configCommand)))),
                                    *///?} else
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, configCommand)))),
                    false);
        });
    }

    public void checkUpdate() {
        try {
            if (!mod.config.updateChecker || EventUtils.MC_VERSION == null) return;

            // Ensure client in-game
            if (MinecraftClient.getInstance().player == null) return;
	
            // Get client and request
            final HttpClient httpClient = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.modrinth.com/v2/project/alerts/version" +
                            "?game_versions=%5B%22" + EventUtils.MC_VERSION + "%22%5D" +
                            "&include_changelog=false"))
                    .header("User-Agent", EventUtils.USER_AGENT)
                    .build();

            // Make request
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> {
                        try {
                            final JsonObject version = JsonParser
                                    .parseString(body).getAsJsonArray()
                                    .get(0).getAsJsonObject();
                            if (version == null) {
                                EventUtils.LOGGER.error("Failed to check for updates: Unexpected response from Modrinth (no version)");
                                return;
                            }

                            // Check if channel is release
                            if (!version.has("version_type")) {
                                EventUtils.LOGGER.error("Failed to check for updates: Unexpected response from Modrinth (no version_type)");
                                return;
                            }
                            if (!version.get("version_type").getAsString().equals("release")) return;

                            // Get version number
                            if (!version.has("version_number")) {
                                EventUtils.LOGGER.error("Failed to check for updates: Unexpected response from Modrinth (no version_number)");
                                return;
                            }
                            final String latestVersion = version.get("version_number").getAsString();

                            // Notify update
                            if (!BuildProperties.MOD_VERSION_FULL.equals(latestVersion)) notifyUpdate(latestVersion);
                        } catch (final Exception e) {
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
