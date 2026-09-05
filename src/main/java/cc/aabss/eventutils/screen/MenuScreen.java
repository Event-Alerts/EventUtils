package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.screen.config.ConfigScreen;
import cc.aabss.eventutils.screen.config.group.GroupManagerScreen;
import cc.aabss.eventutils.sdk.EnrichedPlayer;
import cc.aabss.eventutils.versioning.VersionedClient;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.network.chat.Component.translatable;


public class MenuScreen extends ScreenWithParent<FlowLayout> {
    public MenuScreen(@Nullable Screen parent) {
        super(parent);
    }

    @Override @NotNull
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(@NotNull FlowLayout rootComponent) {
        final EnrichedPlayer player = EventUtils.MOD.authManager.player;

        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        final FlowLayout buttons = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        buttons.horizontalSizing(Sizing.fixed(150));
        buttons.gap(10);

        buttons.child(Components
                .button(translatable("eventutils.menu.config").withStyle(ChatFormatting.GOLD), button -> {
                    new VersionedClient(Minecraft.getInstance()).setScreen(ConfigScreen.getConfigScreen(this));
                })
                .horizontalSizing(Sizing.expand()));

        buttons.child(Components
                .button(translatable("eventutils.menu.group_manager").withStyle(ChatFormatting.YELLOW), button -> {
                    new VersionedClient(Minecraft.getInstance()).setScreen(new GroupManagerScreen(EventUtils.MOD, this));
                })
                .horizontalSizing(Sizing.expand()));

        if (player != null) {
            if (!player.isDiscordLinked()) buttons.child(Components
                    .button(translatable("eventutils.menu.discord_link").withStyle(ChatFormatting.AQUA), button -> {
                        EventUtils.MOD.discordLinkManager.startLink(this, null);
                    })
                    .horizontalSizing(Sizing.expand()));

            if (player.isDiscordLinked()) buttons.child(Components
                    .button(translatable("eventutils.menu.event_builder").withStyle(ChatFormatting.GREEN), button -> {
                        new EventBuilder(this).open();
                    })
                    .horizontalSizing(Sizing.expand()));
        }

        rootComponent.child(buttons);
    }
}
