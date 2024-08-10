package cc.aabss.eventutils.commands;

import cc.aabss.eventutils.EventUtils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public abstract class EventCommand {
    @NotNull protected final EventUtils mod;

    protected EventCommand(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    /**
     * <i>{@code OPTIONAL}</i> This is the name of the command
     * <p>If not specified, the lowercase class name will be used ({@code Command} will be removed)
     * <p><b>Example:</b> the command class {@code MyEpicCommand} would be registered as {@code myepic}
     *
     * @return  the name of the command
     */
    @NotNull
    protected String getName() {
        return getClass().getSimpleName().toLowerCase().replace("command", "").replace("cmd", "");
    }

    /**
     * <i>{@code OPTIONAL}</i> These are the aliases for the command
     * <p>If not specified (or {@code null}), no aliases will be added
     *
     * @return  the aliases for the command
     */
    @Nullable
    protected Collection<String> getAliases() {
        return null;
    }

    /**
     * <i>{@code OPTIONAL}</i> These are the arguments for the command
     * <p>If not specified (or {@code null}), no arguments will be added
     *
     * @return  the arguments for the command
     */
    @Nullable
    protected Collection<ArgumentBuilder<FabricClientCommandSource, ? extends ArgumentBuilder<FabricClientCommandSource, ?>>> getArguments() {
        return null;
    }

    /**
     * This is the method that will be executed when the command is run
     *
     * @param   context the {@link CommandContext} of the command
     */
    protected abstract void run(@NotNull CommandContext<FabricClientCommandSource> context);

    public void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Create full executor with return value
        final Command<FabricClientCommandSource> runFull = context -> {
            run(context);
            return 0;
        };

        // Add executor and arguments
        final LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(getName()).executes(runFull);
        if (getArguments() != null) for (final ArgumentBuilder<FabricClientCommandSource, ?> argumentBuilder : getArguments()) builder.then(argumentBuilder.executes(runFull));

        // Register command
        final LiteralCommandNode<FabricClientCommandSource> command = dispatcher.register(builder);

        // Add aliases
        if (getAliases() != null) for (final String name : getAliases()) dispatcher.register(ClientCommandManager.literal(name).redirect(command));
    }
}
