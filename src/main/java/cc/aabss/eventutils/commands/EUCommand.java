package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jetbrains.annotations.NotNull;


public abstract class EUCommand {
    @NotNull protected final EventUtils mod;

    public EUCommand(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    @NotNull
    public abstract LiteralCommandNode<FabricClientCommandSource> build();
}
