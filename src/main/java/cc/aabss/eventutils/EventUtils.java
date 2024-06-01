package cc.aabss.eventutils;

import cc.aabss.eventutils.api.websocket.WebSocketEvent;
import cc.aabss.eventutils.config.EventUtil;
import cc.aabss.eventutils.config.JsonConfig;
import club.bottomservices.discordrpc.lib.DiscordRPCClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static cc.aabss.eventutils.api.DiscordRPC.discordConnect;

public class EventUtils implements ClientModInitializer {

    public static WebSocketEvent EVENT_POSTED;
    public static WebSocketEvent FAMOUS_EVENTS;
    public static String TEXT = "\n\n Per-server ranks get a higher priority in their respective queues. To receive such a rank, purchase one at\n store.invadedlands.net.\n\nTo leave a queue, use the command: /leavequeue.\n";
    public static WebSocketEvent POTENTIAL_FAMOUS_EVENTS;
    public static final Logger LOGGER = LogManager.getLogger(EventUtils.class);
    public static DiscordRPCClient client = null;
    public static JsonConfig CONFIG = new JsonConfig(new File(FabricLoader.getInstance().getConfigDir().toString(), "eventutils.json"));

    public static String DEFAULT_FAMOUS_IP = CONFIG.loadObject("default-famous-ip", "play.invadedlands.net");
    public static boolean AUTO_TP = CONFIG.loadObject("auto-tp", false);
    public static boolean DISCORD_RPC = CONFIG.loadObject("discord-rpc", true);
    public static boolean SIMPLE_QUEUE_MSG = CONFIG.loadObject("simple-queue-msg", false);
    public static boolean UPDATE_CHECKER = CONFIG.loadObject("update-checker", true);

    public static boolean FAMOUS_EVENT = CONFIG.loadObject("famous-event", true);
    public static boolean POTENTIAL_FAMOUS_EVENT = CONFIG.loadObject("potential-famous-event", true);
    public static boolean MONEY_EVENT = CONFIG.loadObject("money-event", false);
    public static boolean PARTNER_EVENT = CONFIG.loadObject("partner-event", false);
    public static boolean FUN_EVENT = CONFIG.loadObject("fun-event", false);
    public static boolean HOUSING_EVENT = CONFIG.loadObject("housing-event", false);
    public static boolean COMMUNITY_EVENT = CONFIG.loadObject("community-event", false);
    public static boolean CIVILIZATION_EVENT = CONFIG.loadObject("civilization-event", false);


    @Override
    public void onInitializeClient() {
        EVENT_POSTED = new WebSocketEvent(WebSocketEvent.SocketEndpoint.EVENT_POSTED);
        FAMOUS_EVENTS = new WebSocketEvent(WebSocketEvent.SocketEndpoint.FAMOUS_EVENT);
        POTENTIAL_FAMOUS_EVENTS = new WebSocketEvent(WebSocketEvent.SocketEndpoint.POTENTIAL_FAMOUS_EVENT);
        EventUtil.registerEvents();
        discordConnect();
    }

}
