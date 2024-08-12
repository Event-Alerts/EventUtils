package cc.aabss.eventutils;

import com.google.gson.JsonParser;

import net.fabricmc.loader.api.Version;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

import static net.minecraft.text.Text.translatable;


public class UpdateChecker {
    @NotNull private final EventUtils mod;
    @Nullable private Boolean isOutdated = null;
    @Nullable private String latestVersion = null;

    public UpdateChecker(@NotNull final EventUtils mod) {
        this.mod = mod;
    }

    public void notifyUpdate() {
        if (!mod.config.updateChecker || !Boolean.TRUE.equals(isOutdated) || latestVersion == null) return;
        final MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) client.player.sendMessage(Text.literal("§6[EVENTUTILS]§r §e" + EventUtils.translate("eventutils.updatechecker.new")+"§r §7(v" + Versions.EU_VERSION + " -> v" + latestVersion.replace(Versions.MC_VERSION + "-", "") + ")" + "\n")
                    .setStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translatable("eventutils.update_checker.hover")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/alerts/version/" + latestVersion)))
                    .append(Text.literal("§7§o" + EventUtils.translate("eventutils.updatechecker.config"))), false);
        });
    }

    public void checkUpdate() {
        if (isOutdated != null || !mod.config.updateChecker || Versions.MC_VERSION == null || Versions.EU_VERSION == null || Versions.EU_VERSION_SEMANTIC == null) {
            isOutdated = false;
            return;
        }
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        final HttpClient httpClient = HttpClient.newHttpClient();
        try {
            // latestVersion (Modrinth)
            latestVersion = JsonParser.parseString(httpClient.sendAsync(HttpRequest.newBuilder()
                                    .uri(new URI("https://api.modrinth.com/v2/project/alerts/version?game_versions=%5B%22" + Versions.MC_VERSION + "%22%5D"))
                                    .header("User-Agent", "EventUtils/" + Versions.EU_VERSION + " (Minecraft/" + Versions.MC_VERSION + ")").build(), HttpResponse.BodyHandlers.ofString())
                            .get().body()).getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("version_number").getAsString();
            //? if java: >=21
            httpClient.close();

            // isOutdated
            final Version latestVersionSemantic = latestVersion == null ? null : Versions.getSemantic(latestVersion.replace(Versions.MC_VERSION + "-", ""));
            isOutdated = latestVersionSemantic != null && Versions.EU_VERSION_SEMANTIC.compareTo(latestVersionSemantic) < 0;
        } catch (final URISyntaxException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
