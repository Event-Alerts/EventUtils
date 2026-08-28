package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.manager.EventServerManager;
import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;


@Mixin(ServerList.class)
public abstract class ServerListMixin {
    @Shadow public abstract void remove(ServerData server);

    @Shadow public abstract void save();

    @Inject(method = "load", at = @At("TAIL"))
    private void loadFile(CallbackInfo ci) {
        // Remove inactive servers
        boolean removed = false;
        for (final ServerData server : new ArrayList<>(((ServerListAccessor) this).getServers())) {
            // Remove servers that start with the event server prefix but aren't an active event server
            if (server.name.startsWith(EventServerManager.EVENT_SERVER_PREFIX) && !EventUtils.MOD.eventServerManager.isActiveEventServer(server.ip)) {
                remove(server);
                removed = true;
            }
        }
        if (removed) save();
    }
}
