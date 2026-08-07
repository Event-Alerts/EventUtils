package cc.aabss.eventutils.commands.priority;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;


public class PriorityCommon {
    @Nullable @Unmodifiable
    public static List<String> getNamesSorted(@NotNull MinecraftClient client) {
        if (client.world == null || client.player == null) return null;
        return client.world.getPlayers().stream()
                .filter(player -> !EventUtils.isNpc(player.getUuid()))
                .sorted(Comparator.comparingInt(AbstractClientPlayerEntity::getId))
                .map(player -> player.getName().getString())
                .toList();
    }
}
