package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.api.UpdateChecker;
import cc.aabss.eventutils.commands.EventTeleportCommand;
import cc.aabss.eventutils.commands.EventUtilsCommand;
import cc.aabss.eventutils.commands.TestNotificationCommand;
import club.bottomservices.discordrpc.lib.exceptions.DiscordException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cc.aabss.eventutils.EventUtils.*;
import static cc.aabss.eventutils.EventUtils.TEXT;
import static cc.aabss.eventutils.config.HidePlayers.HIDEPLAYERS;
import static cc.aabss.eventutils.config.HidePlayers.HIDEPLAYERSBIND;

public class EventUtil {

    public static List<EntityType<?>> entityTypes = Registries.ENTITY_TYPE.stream().toList();

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
        if (!SIMPLE_QUEUE_MSG) {
            return text;
        }
        String originalString = text.getString();
        if (!originalString.contains(TEXT)) {
            return text;
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

        ClientReceiveMessageEvents.ALLOW_GAME.register(((text, overlay) -> true));
        ClientReceiveMessageEvents.MODIFY_GAME.register(((text, overlay) -> replace(text)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                UpdateChecker.updateCheck();
            } catch (URISyntaxException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        HidePlayers.loadBinds();


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity p = client.player;
            while (HIDEPLAYERSBIND.wasPressed()) {
                HIDEPLAYERS = !HIDEPLAYERS;
                if (p != null) {
                    p.sendMessage(Text.literal((HIDEPLAYERS ? "§aEnabled" : "§cDisabled") + " hide players"), true);
                }
            }
        });
    }

    public static String ip(String event, boolean housing){
        if (housing){
            return "hypixel.net";
        }
        String[] split = removeMarkdown(event).split("\\s+|\\n+");
        List<String> string = new ArrayList<>();
        for (String s : split){
            if (s.contains(".") && !s.contains("/")){
                string.add(s);
            }
        }
        if (string.size() == 1){
            return string.getFirst();
        } else if (string.size() > 1){
            for (String s : string){
                if (validIp(s)) return s;
            }
        }
        return "";
    }

    public static int prize(JsonObject event){
        if (event.has("prize")){
            return Integer.parseInt(event.get("prize").getAsString().replaceAll("\\$", "").replaceAll("€", "").replaceAll("£", "").split(" ")[0]);
        }
        if (event.has("description")) {
            String[] split = removeMarkdown(event.get("description").getAsString().toLowerCase()).split("\\n+");
            for (String s : split) {
                if (s.contains("$") || s.contains("€") || s.contains("£") || s.contains("dollars") || s.contains("prize")) {
                    String[] split2 = s.split(" ");
                    for (String ss : split2) {
                        if (ss.contains("$") || ss.contains("€") || ss.contains("£")) {
                            String prize = ss.replaceAll("\\$", "").replaceAll("€", "").replaceAll("£", "");
                            try {
                                return Integer.parseInt(prize);
                            } catch (NumberFormatException ignored) {}
                        }
                        try {
                            return Integer.parseInt(ss);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return 0;
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
            if (isHousingEvent(message)){
                ip = ip(message.get("description").getAsString(), true);
            } else {
                ip = ip(message.get("description").getAsString(), false);
            }
        }
        if (EventUtils.AUTO_TP) {
            connect(ip);
        }
        return ip;
    }

    public static String connectFamousIP(String message) {
        String ip = ip(message, false);
        if (EventUtils.AUTO_TP) {
            if (Objects.equals(ip, "")) {
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

    public static Screen screen(Screen parent){
        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Event Utils Mod Config"));
        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder().name(Text.literal("General"));
        ConfigCategory.Builder alertsCategory = ConfigCategory.createBuilder().name(Text.literal("Alerts"));
        builder
                .category(generalCategory
                        .option(ListOption.<String>createBuilder()
                                .name(Text.literal("Whitelisted Players"))
                                .description(OptionDescription.of(Text.literal("The names of the players you can see when players are hidden.")))
                                .binding(EventUtils.WHITELISTED_PLAYERS, () -> EventUtils.WHITELISTED_PLAYERS,
                                        newValue -> {
                                            List<String> names = new ArrayList<>();
                                            newValue.forEach(name -> names.add(name.toLowerCase()));
                                            EventUtils.WHITELISTED_PLAYERS = names;
                                            CONFIG.saveObject("whitelisted-players", EventUtils.WHITELISTED_PLAYERS);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(StringControllerBuilder::create)
                                .initial("Skeppy")
                                .build()
                        )
                        .option(Option.<String>createBuilder()
                                .name(Text.literal("Default Famous IP"))
                                .description(OptionDescription.of(Text.literal("The default ip for if a [potential] famous event is pinged with no ip inputted.")))
                                .binding(EventUtils.DEFAULT_FAMOUS_IP, () -> EventUtils.DEFAULT_FAMOUS_IP,
                                        newValue -> {
                                            EventUtils.DEFAULT_FAMOUS_IP = newValue;
                                            CONFIG.saveObject("default-famous-ip", EventUtils.DEFAULT_FAMOUS_IP);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(StringControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Auto Teleport"))
                                .description(OptionDescription.of(Text.literal("Whether you should be automatically teleported to the server where the event resides.")))
                                .binding(EventUtils.AUTO_TP, () -> EventUtils.AUTO_TP,
                                        newValue -> {
                                            EventUtils.AUTO_TP = newValue;
                                            CONFIG.saveObject("auto-tp", EventUtils.AUTO_TP);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(BooleanControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Discord RPC"))
                                .description(OptionDescription.of(Text.literal("Whether the Discord rich presence should be shown.")))
                                .binding(EventUtils.DISCORD_RPC, () -> EventUtils.DISCORD_RPC,
                                        newValue -> {
                                            EventUtils.DISCORD_RPC = newValue;
                                            CONFIG.saveObject("discord-rpc", EventUtils.DISCORD_RPC);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                            if (client != null) {
                                                try {
                                                    if (newValue)
                                                        EventUtils.client.connect();
                                                    else
                                                        EventUtils.client.disconnect();
                                                } catch (DiscordException ignored){}
                                            }
                                        })
                                .controller(BooleanControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Simplified Queue Message"))
                                .description(OptionDescription.of(Text.literal("Whether the queue message for InvadedLands should be simplified.")))
                                .binding(EventUtils.SIMPLE_QUEUE_MSG, () -> EventUtils.SIMPLE_QUEUE_MSG,
                                        newValue -> {
                                            EventUtils.SIMPLE_QUEUE_MSG = newValue;
                                            CONFIG.saveObject("simple-queue-msg", EventUtils.SIMPLE_QUEUE_MSG);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(BooleanControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Confirm Window Close"))
                                .description(OptionDescription.of(Text.literal("Whether a confirmation should pop up confirming you want to close your game window.")))
                                .binding(EventUtils.CONFIRM_WINDOW_CLOSE, () -> EventUtils.CONFIRM_WINDOW_CLOSE,
                                        newValue -> {
                                            EventUtils.CONFIRM_WINDOW_CLOSE = newValue;
                                            CONFIG.saveObject("confirm-window-close", EventUtils.CONFIRM_WINDOW_CLOSE);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(BooleanControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Confirm Disconnect"))
                                .description(OptionDescription.of(Text.literal("Whether a confirmation should pop up confirming you want to leave your server.")))
                                .binding(EventUtils.CONFIRM_DISCONNECT, () -> EventUtils.CONFIRM_DISCONNECT,
                                        newValue -> {
                                            EventUtils.CONFIRM_DISCONNECT = newValue;
                                            CONFIG.saveObject("confirm-disconnect", EventUtils.CONFIRM_DISCONNECT);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(BooleanControllerBuilder::create)
                                .build()
                        )
                        .option(ListOption.<String>createBuilder()
                                .name(Text.literal("Hidden Entity Types"))
                                .description(OptionDescription.of(Text.literal("The types of entities that will be hidden.")))
                                .binding(EventUtils.HIDDEN_ENTITY_TYPES.stream()
                                                .map(entityType -> EntityType.getId(entityType).toString())
                                                .collect(Collectors.toList()),
                                        () -> EventUtils.HIDDEN_ENTITY_TYPES.stream()
                                                .map(entityType -> EntityType.getId(entityType).toString())
                                                .collect(Collectors.toList()),
                                        newValue -> {
                                            EventUtils.HIDDEN_ENTITY_TYPES = newValue.stream()
                                                    .map(id -> {
                                                        Optional<EntityType<?>> entityType = EntityType.get(id);
                                                        return entityType.orElse(null);
                                                    })
                                                    .collect(Collectors.toList());
                                            CONFIG.saveObject("hidden-entity-types", EventUtils.HIDDEN_ENTITY_TYPES);
                                            CONFIG.saveConfig(CONFIG.JSON);
                                        })
                                .controller(option -> DropdownStringControllerBuilder.create(option)
                                        .values(EventUtil.entityTypes.stream()
                                                .map(entityType -> EntityType.getId(entityType).toString())
                                                .collect(Collectors.toList())
                                        )
                                )
                                .initial(EntityType.getId(EntityType.ALLAY).toString())
                                .build()
                        )
                        .build())
                .category(alertsCategory
                        .option(alertEntry("Famous Events", EventUtils.FAMOUS_EVENT, newValue -> EventUtils.FAMOUS_EVENT = newValue))
                        .option(alertEntry("Potential Famous Events", EventUtils.POTENTIAL_FAMOUS_EVENT, newValue -> EventUtils.POTENTIAL_FAMOUS_EVENT = newValue))
                        .option(alertEntry("Money Events", EventUtils.MONEY_EVENT, newValue -> EventUtils.MONEY_EVENT = newValue))
                        .option(alertEntry("Partner Events", EventUtils.PARTNER_EVENT, newValue -> EventUtils.PARTNER_EVENT = newValue))
                        .option(alertEntry("Fun Events", EventUtils.FUN_EVENT, newValue -> EventUtils.FUN_EVENT = newValue))
                        .option(alertEntry("Housing Events", EventUtils.HOUSING_EVENT, newValue -> EventUtils.HOUSING_EVENT = newValue))
                        .option(alertEntry("Community Events", EventUtils.COMMUNITY_EVENT, newValue -> EventUtils.COMMUNITY_EVENT = newValue))
                        .option(alertEntry("Civilization Events", EventUtils.CIVILIZATION_EVENT, newValue -> EventUtils.CIVILIZATION_EVENT = newValue))
                        .build()
                );
        return builder.build().generateScreen(parent);
    }

    private static Option<Boolean> alertEntry(
            String name,
            Boolean booleanToggle,
            Consumer<Boolean> consumer
    ){
        return Option.<Boolean>createBuilder()
                .name(Text.literal(name))
                .description(OptionDescription.of(Text.literal("Whether you should be pinged for "+name.toLowerCase())))
                .binding(booleanToggle, () -> booleanToggle,
                        consumer
                                .andThen(newValueue -> CONFIG.saveConfig(CONFIG.JSON))
                                .andThen(newValueue -> CONFIG.saveObject(name.replaceAll("s$", ""), newValueue)))
                .controller(BooleanControllerBuilder::create)
                .build();
    }
}