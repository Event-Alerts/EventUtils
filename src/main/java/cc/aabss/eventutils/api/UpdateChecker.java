package cc.aabss.eventutils.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static cc.aabss.eventutils.EventUtils.UPDATE_CHECKER;

public class UpdateChecker {

    public static void updateCheck() throws URISyntaxException, ExecutionException, InterruptedException {
        if (!UPDATE_CHECKER){
            return;
        }
        String ver = getVer("eventutils");
        String mc = getVer("minecraft");
        JsonArray json = JsonParser.parseString(HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder()
                        .uri(new URI("https://api.modrinth.com/v2/project/alerts/version?game_versions=%5B%22"+mc+"%22%5D")).build(), HttpResponse.BodyHandlers.ofString())
                .get().body()).getAsJsonArray();
        String newVer = json.get(0).getAsJsonObject().get("version_number").getAsString();
        if (!Objects.equals(newVer, ver)) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    ClientPlayerEntity p = client.player;
                    p.sendMessage(
                            Text.literal("§6[EVENTUTILS]§r §eThere is a new update available!§r §7(v" + ver.replaceAll(mc+"-", "") + " -> v" + newVer.replaceAll(mc+"-", "") + ")")
                                    .setStyle(
                                            Style.EMPTY.withHoverEvent(
                                                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§eClick to open download."))
                                            ).withClickEvent(
                                                    new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/alerts/version/" + newVer)
                                            )
                                    ),
                            false);
                }
            });
        }

    }

    private static String getVer(String id){
        if (FabricLoader.getInstance().getModContainer(id).isPresent()) {
            return FabricLoader.getInstance().getModContainer(id).get().getMetadata().getVersion().getFriendlyString();
        }
        return "";
    }
}
