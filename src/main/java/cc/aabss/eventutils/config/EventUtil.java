package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.api.UpdateChecker;
import cc.aabss.eventutils.commands.EventTeleportCommand;
import cc.aabss.eventutils.commands.EventUtilsCommand;
import cc.aabss.eventutils.commands.TestNotificationCommand;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static cc.aabss.eventutils.EventUtils.*;
import static cc.aabss.eventutils.EventUtils.TEXT;

public class EventUtil {

    public static void connect(String ip){
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            LOGGER.error("Minecraft client instance is null");
            return;
        }
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address.equalsIgnoreCase(ip)){
            LOGGER.warn("Already in server.");
            return;
        }
        client.execute(() -> {
            try {
                client.disconnect();
                ServerAddress serverAddress = ServerAddress.parse(ip);
                ConnectScreen.connect(new TitleScreen(), client, serverAddress,
                        new ServerInfo("EventUtils Event Server", ip, ServerInfo.ServerType.OTHER),
                        true, null);
            } catch (Exception e) {
                LOGGER.error("Failed to connect to server: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public static Text replace(Text text){
        Text originalText = text;
        if (!SIMPLE_QUEUE_MSG) {
            return originalText;
        }
        String originalString = originalText.getString();
        if (!originalString.contains(TEXT)) {
            return originalText;
        }
        String modifiedString = originalString.replace(TEXT, "").replaceFirst("\n", "");
        String[] lines = modifiedString.split("\n");
        MutableText resultText = Text.literal("");
        for (String line : lines) {
            String[] parts = line.split(": ");
            if (parts.length > 1) {
                String keyPart = parts[0];
                String[] valueParts = parts[1].split("/");
                MutableText first = Text.literal(keyPart).formatted(Formatting.GOLD);
                MutableText second = Text.literal(valueParts[0]).formatted(Formatting.YELLOW);
                MutableText third = Text.literal("/").formatted(Formatting.GOLD);
                MutableText fourth = Text.literal(valueParts[1]).formatted(Formatting.YELLOW);
                MutableText formattedLine = first.append(": ").append(second).append(third).append(fourth);
                if (!resultText.getSiblings().isEmpty()) {
                    resultText.append("\n");
                }
                resultText.append(formattedLine);
            } else {
                resultText.append(Text.literal(line).formatted(Formatting.GOLD));
            }
        }
        return resultText;
    }

    public static void registerEvents(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            EventTeleportCommand.register(dispatcher);
            EventUtilsCommand.register(dispatcher);
            TestNotificationCommand.register(dispatcher);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            EVENT_POSTED.webSocket.sendClose(1000, "EventUtils client ("+client.getSession().getUsername()+") closed");
            FAMOUS_EVENTS.webSocket.sendClose(1000, "EventUtils client ("+client.getSession().getUsername()+") closed");
            POTENTIAL_FAMOUS_EVENTS.webSocket.sendClose(1000, "EventUtils client ("+client.getSession().getUsername()+") closed");
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register(((text, overlay) -> {
            if (text.copyContentOnly().contains(Text.of(TEXT))){
                return !SIMPLE_QUEUE_MSG;
            }
            return true;
        }));
        ClientReceiveMessageEvents.MODIFY_GAME.register(((text, overlay) -> replace(text)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                UpdateChecker.updateCheck();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String ip(String event){
        String[] split = removeMarkdown(event).split("\\s+|\\n+");
        List<String> string = new ArrayList<>();
        for (String s : split){
            if (s.contains(".") && !s.contains("/")){
                string.add(s);
            }
        }
        if (string.size() == 1){
            return string.get(0);
        } else if (string.size() > 1){
            for (String s : string){
                if (validIp(s)) return s;
            }
        }
        return null;
    }

    public static String getImage(byte[] bytes) {
        return "data:image/png;base64," + new String(Base64.getUrlEncoder().encode(bytes));
    }

    public static boolean validIp(String ip){
        try {
            HttpRequest req = HttpRequest.newBuilder(new URI("https://api.mcstatus.io/v2/status/java/"+ip)).build();
            String body = HttpClient.newHttpClient().sendAsync(req, HttpResponse.BodyHandlers.ofString()).get().body();
            return !body.endsWith(":null}") && !body.endsWith("Not Found") && !body.endsWith("Invalid address value");
        } catch (ExecutionException | InterruptedException | URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    public static String removeMarkdown(String string) {
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

    public static String getAndConnectIP(JsonObject message) {
        String ip = "";
        if (message.has("ip")) {
            ip = message.get("ip").getAsString();
        } else if (message.has("description")) {
            ip = ip(message.get("description").getAsString());
        }
        if (EventUtils.AUTO_TP) {
            connect(ip);
        }
        return ip;
    }

    public static String connectFamousIP(String message) {
        String ip = ip(message);
        if (EventUtils.AUTO_TP) {
            if (ip == null) {
                connect(EventUtils.DEFAULT_FAMOUS_IP);
            } else {
                connect(ip);
            }
        }
        return ip;
    }

    public static boolean isPartnerEvent(JsonObject message) {
        return templateBool(message, EventUtils.PARTNER_EVENT, "970434201990070424");
    }

    public static boolean isCommunityEvent(JsonObject message) {
        return templateBool(message, EventUtils.COMMUNITY_EVENT, "980950599946362900");
    }

    public static boolean isMoneyEvent(JsonObject message) {
        return templateBool(message, EventUtils.MONEY_EVENT, "970434305203511359");
    }

    public static boolean isFunEvent(JsonObject message) {
        return templateBool(message, EventUtils.FUN_EVENT, "970434303391576164");
    }

    public static boolean isHousingEvent(JsonObject message) {
        return templateBool(message, EventUtils.HOUSING_EVENT, "970434294893928498");
    }

    public static boolean isCivilizationEvent(JsonObject message) {
        return templateBool(message, EventUtils.CIVILIZATION_EVENT, "1134932175821734119");
    }

    public static boolean templateBool(JsonObject message, boolean bool, String id){
        if (message.has("roles") && bool){
            JsonArray array = message.getAsJsonArray("roles");
            for (int i = 0; i < array.size(); i++){
                if (array.get(i).getAsLong() == Long.parseLong(id)){
                    return true;
                }
            }
        }
        return false;
    }
}