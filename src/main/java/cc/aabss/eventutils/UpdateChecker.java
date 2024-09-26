package cc.aabss.eventutils;

import com.google.gson.JsonParser;

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

import static net.minecraft.text.Text.translatable;


public class UpdateChecker {
    @NotNull private final EventUtils mod;
    @Nullable private String latestVersion = null;

    public UpdateChecker(@NotNull final EventUtils mod) {
        this.mod = mod;
    }

    private void notifyUpdate() {
        System.out.println(7);
        final MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> {
            if (client.player != null) client.player.sendMessage(Text.literal("§6[EVENTUTILS]§r §e" + EventUtils.translate("eventutils.updatechecker.new")+"§r §7(v" + Versions.EU_VERSION + " -> v" + latestVersion.replace(Versions.MC_VERSION + "-", "") + ")" + "\n")
                    .setStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translatable("eventutils.updatechecker.hover")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/alerts/version/" + latestVersion)))
                    .append(Text.literal("§7§o" + EventUtils.translate("eventutils.updatechecker.config"))
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eventutils config")))
                    ), false);
        });
    }

    public void checkUpdate() {
        if (!mod.config.updateChecker || Versions.MC_VERSION == null || Versions.EU_VERSION == null || Versions.EU_VERSION_SEMANTIC == null) {
            System.out.println(1);
            return;
        }
        System.out.println(2);
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            System.out.println(3);
            return;
        }
        final HttpClient httpClient = HttpClient.newHttpClient();
        try {
            // latestVersion (Modrinth)
            httpClient.sendAsync(HttpRequest.newBuilder()
                            .uri(new URI("https://api.modrinth.com/v2/project/alerts/version?game_versions=%5B%22" + Versions.MC_VERSION + "%22%5D"))
                            .header("User-Agent", "EventUtils/" + Versions.EU_VERSION + " (Minecraft/" + Versions.MC_VERSION + ")").build(), HttpResponse.BodyHandlers.ofString())
                    .whenCompleteAsync((response, throwable) -> {
                        System.out.println(4);
                        latestVersion = JsonParser.parseString(response.body()).getAsJsonArray()
                                .get(0).getAsJsonObject()
                                .get("version_number").getAsString();
                        httpClient.close();
                        assert latestVersion != null;
                        if (!latestVersion.equals(Versions.MC_VERSION+"-"+Versions.EU_VERSION)) {
                            System.out.println(5);
                            notifyUpdate();
                        }
                        System.out.println(6);
                    });
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
