package cc.aabss.eventutils.discordrpc;

import cc.aabss.eventutils.EventUtils;
import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;
import org.jetbrains.annotations.NotNull;
import xyz.srnyx.javautilities.MiscUtility;

import java.util.concurrent.TimeUnit;


public class CustomIPCListener implements IPCListener {
    @NotNull private final DiscordRPC rpc;

    public CustomIPCListener(@NotNull DiscordRPC rpc) {
        this.rpc = rpc;
    }

    @Override
    public void onReady(@NotNull IPCClient client) {
        EventUtils.LOGGER.debug("[DISCORD RPC] onReady");
        MiscUtility.IO_SCHEDULER.scheduleAtFixedRate(rpc::refresh, 0, DiscordRPC.REFRESH_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void onPacketSent(@NotNull IPCClient client, Packet packet) {}
    @Override
    public void onPacketReceived(@NotNull IPCClient client, Packet packet) {}
    @Override
    public void onActivityJoin(@NotNull IPCClient client, String secret) {}
    @Override
    public void onActivitySpectate(@NotNull IPCClient client, String secret) {}
    @Override
    public void onActivityJoinRequest(@NotNull IPCClient client, String secret, User user) {}
    @Override
    public void onClose(@NotNull IPCClient client, JsonObject json) {}
    @Override
    public void onDisconnect(@NotNull IPCClient client, Throwable t) {}
}
