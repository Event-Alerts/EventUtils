package cc.aabss.eventutils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.text.Text.translatable;


/**
 * Only fetch latest version once per session
 */
public class UpdateChecker {
    @NotNull private final EventUtils mod;

    private boolean fetched = false;
    /**
     * {@code null} if on latest version
     */
    @Nullable private String latestVersion;

    public UpdateChecker(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    public void notifyUpdate() {
        if (!mod.config.update_checker) return;

        // Ensure client in-game
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        fetchLatestVersion().thenAccept(version -> {
            // Only notify if not on latest
            if (version != null) client.send(() -> {
                if (client.player == null) return;
                final MutableText hover = translatable("eventutils.updatechecker.hover");
                final String link = "https://modrinth.com/mod/alerts/version/" + version;
                final String configCommand = "/eventutils config";
                client.player.sendMessage(
                        EventUtils.MESSAGE_PREFIX.copy().append(" §e" + EventUtils.translate("eventutils.updatechecker.new") + "§r §7(" + BuildProperties.MOD_VERSION + " -> v" + version.replace(EventUtils.MC_VERSION + "-", "") + ")" + "\n")
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
        });
    }

    @NotNull
    private CompletableFuture<String> fetchLatestVersion() {
        // Already fetched
        if (fetched) return CompletableFuture.completedFuture(latestVersion);

        if (EventUtils.MC_VERSION == null) {
            latestVersion = null;
            return CompletableFuture.completedFuture(null);
        }

        // Fetch
        try {
            // Get request
            final String url = "https://api.modrinth.com/v2/project/alerts/version" +
                    "?game_versions=%5B%22" + EventUtils.MC_VERSION + "%22%5D" +
                    "&include_changelog=false";
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("User-Agent", EventUtils.USER_AGENT)
                    .build();

            // Make request
            return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(body -> {
                        fetched = true;

                        try {
                            // Parse versions array
                            final JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
                            if (versions == null || versions.isEmpty()) {
                                EventUtils.LOGGER.debug("No updates found for {}", url);
                                return null;
                            }

                            // Get version
                            final JsonObject version = versions.get(0).getAsJsonObject();
                            if (version == null) {
                                EventUtils.LOGGER.warn("Failed to fetch latest version: Unexpected response from Modrinth (no version)");
                                return null;
                            }

                            // Check if channel is release
                            if (!version.has("version_type")) {
                                EventUtils.LOGGER.warn("Failed to fetch latest version: Unexpected response from Modrinth (no version_type)");
                                return null;
                            }
                            if (!version.get("version_type").getAsString().equals("release")) return null;

                            // Get version number
                            if (!version.has("version_number")) {
                                EventUtils.LOGGER.warn("Failed to fetch latest version: Unexpected response from Modrinth (no version_number)");
                                return null;
                            }
                            final String versionNumber = version.get("version_number").getAsString();

                            // Already on latest = null
                            latestVersion = BuildProperties.MOD_VERSION_FULL.equals(versionNumber) ? null : versionNumber;
                            return latestVersion;
                        } catch (final Exception e) {
                            EventUtils.LOGGER.warn("Failed to parse update check for {}: {}", url, body, e);
                        }
                        return null;
                    })
                    .exceptionally(e -> {
                        EventUtils.LOGGER.warn("Failed to fetch latest version", e);
                        return null;
                    });
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to fetch latest version", e);
            return CompletableFuture.completedFuture(null);
        }
    }
}
