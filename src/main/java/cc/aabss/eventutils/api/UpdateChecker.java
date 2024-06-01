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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static cc.aabss.eventutils.EventUtils.UPDATE_CHECKER;

public class UpdateChecker {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void updateCheck() throws URISyntaxException {
        if (!UPDATE_CHECKER){
            return;
        }
        String ver = FabricLoader.getInstance().getModContainer("eventutils").get().getMetadata().getVersion().getFriendlyString();
        String mc = FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
        ver = ver.replaceAll(mc+"-", "");
        String finalVer = ver;
        AtomicBoolean atomicBool = new AtomicBoolean(false);
        AtomicReference<String> atomicNewVer = new AtomicReference<>();
        HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder()
                        .uri(new URI("https://api.modrinth.com/v2/project/alerts/version")).build(), HttpResponse.BodyHandlers.ofString())
                .whenComplete((stringHttpResponse, throwable) -> {
                    JsonArray json = JsonParser.parseString(stringHttpResponse.body()).getAsJsonArray();
                    json.asList().forEach(jsonElement ->
                            jsonElement.getAsJsonObject().get("game_versions").getAsJsonArray().forEach(jsonElement1 -> {
                                if (jsonElement1.getAsString().equals(mc)) {
                                    if (Integer.parseInt(jsonElement.getAsJsonObject().get("version_number").getAsString().replaceAll(mc+"-", "")) > Integer.parseInt(finalVer)) {
                                        atomicBool.set(true);
                                        atomicNewVer.set(jsonElement.getAsJsonObject().get("version_number").getAsString());
                                    }
                                }
                            }
                    ));
                });
        boolean bool = atomicBool.get();
        if (bool){
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                ClientPlayerEntity p = client.player;
                p.sendMessage(
                        Text.literal("§6[EVENTUTILS]§r §eThere is a new update available!§r §7(v" + finalVer + " -> v" + atomicNewVer.get() + ")")
                                .setStyle(
                                        Style.EMPTY.withHoverEvent(
                                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§eClick to open download."))
                                        ).withClickEvent(
                                                new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/alerts/version/" + atomicNewVer.get())
                                        )
                                ),
                        false);
            }
        }
    }
}
