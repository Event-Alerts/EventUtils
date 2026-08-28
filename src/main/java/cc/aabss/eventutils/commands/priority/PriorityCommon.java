package cc.aabss.eventutils.commands.priority;

import cc.aabss.eventutils.EventUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;


public class PriorityCommon {
    @Nullable @Unmodifiable
    public static List<String> getNamesSorted(@NotNull Minecraft client) {
        if (client.level == null || client.player == null) return null;
        return client.level.players().stream()
                .filter(player -> !EventUtils.isNpc(player.getUUID()))
                .sorted(Comparator.comparingInt(AbstractClientPlayer::getId))
                .map(player -> player.getName().getString())
                .toList();
    }
}
