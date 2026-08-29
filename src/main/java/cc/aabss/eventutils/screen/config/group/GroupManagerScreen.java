package cc.aabss.eventutils.screen.config.group;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.versioning.VersionedClient;
import cc.aabss.eventutils.versioning.VersionedGraphicsGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;


public class GroupManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int PADDING = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int REMOVE_WIDTH = 50;

    @NotNull private final EventUtils mod;
    @Nullable private final Screen parent;

    public GroupManagerScreen(@NotNull EventUtils mod, @Nullable Screen parent) {
        super(translatable("eventutils.config.groups.manage.label"));
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    protected void init() {
        final VersionedClient vClient = new VersionedClient(minecraft);
        final int listTop = 40;

        int i = 0;
        for (final Group group : mod.config.groups.values()) {
            final int y = listTop + i * ROW_HEIGHT;
            if (y >= height - 60) break;
            i++;

            final Button editBtn = Button.builder(literal(group.getName()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE0E0E0))), button -> {
                if (minecraft != null) vClient.setScreen(EditGroupScreen.getScreen(mod, this, group));
            }).bounds(PADDING, y, width - PADDING * 2 - REMOVE_WIDTH - 4, 20).build();
            addRenderableWidget(editBtn);

            addRenderableWidget(Button.builder(literal("X").withStyle(ChatFormatting.RED), button -> {
                if (mod.groupManager.selectedGroup == group.getUuid()) mod.groupManager.selectedGroup = null;

                mod.config.groups.remove(group.getUuid());
                mod.config.save();
                if (minecraft != null) vClient.setScreen(new GroupManagerScreen(mod, parent));
            }).bounds(width - PADDING - REMOVE_WIDTH, y, REMOVE_WIDTH, 20).build());
        }

        final Button addBtn = Button.builder(translatable("eventutils.config.groups.add"), button -> {
            mod.config.upsertGroup(new Group());
            if (minecraft != null) vClient.setScreen(new GroupManagerScreen(mod, parent));
        }).bounds(width / 2 - BUTTON_WIDTH - 4, height - 32, BUTTON_WIDTH, 20).build();
        addRenderableWidget(addBtn);

        addRenderableWidget(Button.builder(translatable("gui.done"), button -> goBack())
                .bounds(width / 2 + 4, height - 32, BUTTON_WIDTH, 20).build());
    }

    private void goBack() {
        if (minecraft != null) new VersionedClient(minecraft).setScreen(parent);
    }

    @Override
    //? if >=26.1 {
    /*public void extractRenderState(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    *///?} else {
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //?}
        final VersionedGraphicsGui vGraphics = new VersionedGraphicsGui(graphics);
        graphics.fill(0, 0, width, height, 0xC0101010);
        vGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        //? if >=26.1 {
        /*super.extractRenderState(graphics, mouseX, mouseY, delta);
        *///?} else {
        super.render(graphics, mouseX, mouseY, delta);
        //?}
    }
}
