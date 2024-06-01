package cc.aabss.eventutils.api;

import cc.aabss.eventutils.EventUtils;
import club.bottomservices.discordrpc.lib.DiscordRPCClient;
import club.bottomservices.discordrpc.lib.EventListener;
import club.bottomservices.discordrpc.lib.RichPresence;
import club.bottomservices.discordrpc.lib.User;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import static cc.aabss.eventutils.EventUtils.LOGGER;

public class DiscordRPC {

    public static void discordConnect(){
        login();
        EventUtils.client.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (EventUtils.client.isConnected) {
                EventUtils.client.disconnect();
            }
        }, "YARPC Shutdown Hook"));
    }

    public static void login(){
        MinecraftClient mcclient = MinecraftClient.getInstance();
        long start = System.currentTimeMillis()/1000;
        EventUtils.client = new DiscordRPCClient(new EventListener() {
            @Override
            public void onReady(@NotNull DiscordRPCClient client, @NotNull User user) {
                LOGGER.info("[DISCORD] Logged in as {}#{}", user.username, user.discriminator);
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        RichPresence presence = new RichPresence.Builder()
                                .addButton("Event Alerts Mod", "https://modrinth.com/mod/alerts")
                                .addButton("Event Alerts Discord", "https://discord.gg/uFPFNYzAWC")
                                .setTimestamps(start, null)
                                .setText("Playing as " + mcclient.getSession().getUsername(), "Currently in " + getCurrentAction(false))
                                .setAssets("event_alerts", "logo by Bansed",
                                        getCurrentAction(true), DiscordRPC.ver())
                                .build();
                        client.sendPresence(presence);
                    }
                }, 0, 5000);
            }
        }, "1236917260036083743");
    }

    public static String getCurrentAction(boolean asset) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null && client.getServer().isRunning()) {
            return asset ? "singleplayer" : "Singleplayer";
        } else if (client.getCurrentServerEntry() != null) {
            return asset ? "https://api.mcstatus.io/v2/icon/"+client.getCurrentServerEntry().address : "Multiplayer";
        } else {
            return asset ? "themainmenu" : "the Main Menu";
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static String ver() {
        return "Minecraft v"+FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
    }

}
