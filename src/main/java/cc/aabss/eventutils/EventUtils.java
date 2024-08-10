package cc.aabss.eventutils;

import cc.aabss.eventutils.commands.EventCommand;
import cc.aabss.eventutils.websocket.SocketEndpoint;
import cc.aabss.eventutils.websocket.WebSocketClient;
import cc.aabss.eventutils.config.EventConfig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.Version;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EventUtils implements ClientModInitializer {
    /**
     * Only use if it is absolutely impossible to access the mod instance through other (safer) means
     * <br>This is usually only necessary for mixins!
     */
    public static EventUtils MOD;
    @NotNull public static final Logger LOGGER = LogManager.getLogger(EventUtils.class);
    @NotNull public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
    @NotNull public static String QUEUE_TEXT = "\n\n Per-server ranks get a higher priority in their respective queues. To receive such a rank, purchase one at\n store.invadedlands.net.\n\nTo leave a queue, use the command: /leavequeue.\n";

    @NotNull public final EventConfig config = new EventConfig();
    @NotNull public DiscordRPC discordRPC = new DiscordRPC(this);
    @NotNull public Map<EventType, String> lastIps = new EnumMap<>(EventType.class);
    public boolean hidePlayers = false;
    @Nullable private Boolean isOutdated = null;
    @Nullable private String latestVersion = null;

    public EventUtils() {
        MOD = this;
    }

    @Override
    public void onInitializeClient() {
        discordRPC.connect();
        updateCheck();

        // Websockets
        final Set<WebSocketClient> webSockets = new HashSet<>();
        webSockets.add(new WebSocketClient(this, SocketEndpoint.EVENT_POSTED));
        webSockets.add(new WebSocketClient(this, SocketEndpoint.FAMOUS_EVENT));
        webSockets.add(new WebSocketClient(this, SocketEndpoint.POTENTIAL_FAMOUS_EVENT));

        // Commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            new EventCommand(dispatcher);
        });

        // Game closed
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            webSockets.forEach(WebSocketClient::close);
            discordRPC.disconnect();
        });

        // Notify of update when joining server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (config.updateChecker && Boolean.TRUE.equals(isOutdated) && latestVersion != null) client.execute(() -> {
                if (client.player != null)
                    client.player.sendMessage(Text.literal("§6[EVENTUTILS]§r §eThere is a new update available!§r §7(v" + Versions.EU_VERSION + " -> v" + latestVersion.replace(Versions.MC_VERSION + "-", "") + ")" + "\n")
                            .setStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§eClick to open download.")))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/alerts/version/" + latestVersion)))
                            .append(Text.literal("§7§oYou can disable this message in the config")), false);
            });
        });

        // Hide players keybind
        final KeyBinding hidePlayersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eventutils.hideplayers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                "key.category.eventutils"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hidePlayersKey.wasPressed()) {
                hidePlayers = !hidePlayers;
                if (client.player != null) client.player.sendMessage(Text.literal((hidePlayers ? "§aEnabled" : "§cDisabled") + " hide players"), true);
            }
        });

        // Simple queue message
        ClientReceiveMessageEvents.ALLOW_GAME.register(((text, overlay) -> true));
        ClientReceiveMessageEvents.MODIFY_GAME.register(((text, overlay) -> {
            if (!config.simpleQueueMessage) return text;
            final String original = text.getString();
            if (!original.contains(QUEUE_TEXT)) return text;
            final MutableText resultText = Text.literal("");
            for (final String line : original.replace(QUEUE_TEXT, "").replaceFirst("\n", "").split("\n")) {
                final String[] parts = line.split(": ");
                if (parts.length <= 1) {
                    resultText.append(Text.literal(line).formatted(Formatting.GOLD));
                    continue;
                }
                if (!resultText.getSiblings().isEmpty()) resultText.append("\n");
                final String[] valueParts = parts[1].split("/");
                resultText.append(Text.literal(parts[0]).formatted(Formatting.GOLD).append(": ")
                        .append(Text.literal(valueParts[0]).formatted(Formatting.YELLOW))
                        .append(Text.literal("/").formatted(Formatting.GOLD))
                        .append(Text.literal(valueParts[1]).formatted(Formatting.YELLOW)));
            }
            return resultText;
        }));
    }

    public void updateCheck() {
        if (isOutdated != null || !config.updateChecker || Versions.MC_VERSION == null || Versions.EU_VERSION == null || Versions.EU_VERSION_SEMANTIC == null) {
            isOutdated = false;
            return;
        }
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) try (final HttpClient httpClient = HttpClient.newHttpClient()) {
            // latestVersion (Modrinth)
            latestVersion = JsonParser.parseString(httpClient.sendAsync(HttpRequest.newBuilder()
                                    .uri(new URI("https://api.modrinth.com/v2/project/alerts/version?game_versions=%5B%22" + Versions.MC_VERSION + "%22%5D"))
                                    .header("User-Agent", "EventUtils/" + Versions.EU_VERSION + " (Minecraft/" + Versions.MC_VERSION + ")").build(), HttpResponse.BodyHandlers.ofString())
                            .get().body()).getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("version_number").getAsString();

            // isOutdated
            final Version latestVersionSemantic = Versions.getSemantic(latestVersion.replaceAll(Versions.MC_VERSION + "-", ""));
            isOutdated = latestVersionSemantic != null && Versions.EU_VERSION_SEMANTIC.compareTo(latestVersionSemantic) < 0;
        } catch (final URISyntaxException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public String getIpAndConnect(@NotNull EventType eventType, @NotNull Object message) {
        // Check if Famous/Potential Famous
        if (message instanceof String messageString) {
            final String ip = ConnectUtility.getIp(messageString);
            if (config.autoTp) ConnectUtility.connect(ip == null ? config.defaultFamousIp : ip);
            return ip;
        }
        if (!(message instanceof JsonObject messageJson)) return null;

        // Get IP
        String ip = "hypixel.net";
        if (eventType != EventType.HOUSING) {
            final JsonElement messageIp = messageJson.get("ip");
            if (messageIp != null) { // Specifically provided
                ip = messageIp.getAsString();
            } else { // Extract from description
                final JsonElement messageDescription = messageJson.get("description");
                if (messageDescription != null) ip = ConnectUtility.getIp(messageDescription.getAsString());
            }
        }

        // Auto TP if enabled
        if (config.autoTp && ip != null) ConnectUtility.connect(ip);
        return ip;
    }
}
