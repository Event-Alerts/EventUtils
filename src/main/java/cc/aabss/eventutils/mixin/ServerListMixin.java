package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.manager.EventServerManager;
import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;


@Mixin(ServerList.class)
public abstract class ServerListMixin {
    @Shadow public abstract void remove(ServerInfo server);

    @Shadow public abstract void saveFile();

    @Inject(method = "loadFile", at = @At("TAIL"))
    private void loadFile(CallbackInfo ci) {
        // Remove inactive servers
        boolean removed = false;
        for (final ServerInfo server : new ArrayList<>(((ServerListAccessor) this).getServers())) {
            // Remove servers that start with the event server prefix but aren't an active event server
            if (server.name.startsWith(EventServerManager.EVENT_SERVER_PREFIX) && !EventUtils.MOD.eventServerManager.isActiveEventServer(server.address)) {
                remove(server);
                removed = true;
            }
        }
        if (removed) saveFile();
    }
}
