package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import static net.minecraft.text.Text.literal;
import static net.minecraft.text.Text.translatable;


public class GroupManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int PADDING = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int REMOVE_WIDTH = 50;

    @Nullable private final Screen parent;

    public GroupManagerScreen(@Nullable Screen parent) {
        super(translatable("eventutils.config.groups.manage_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        final EventConfig config = EventUtils.MOD.config;
        final int listTop = 40;

        for (int i = 0; i < config.groups.size(); i++) {
            final int index = i;
            final PlayerGroup group = config.groups.get(i);
            final int y = listTop + i * ROW_HEIGHT;
            if (y >= height - 60) break;

            final ButtonWidget editBtn = ButtonWidget.builder(
                    literal(group.getName()).fillStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE0E0E0))),
                    button -> client.setScreen(new EditGroupScreen(this, index))
            ).dimensions(PADDING, y, width - PADDING * 2 - REMOVE_WIDTH - 4, 20).build();
            addDrawableChild(editBtn);

            addDrawableChild(ButtonWidget.builder(literal("X").formatted(Formatting.RED), button -> {
                config.groups.remove(index);
                config.setSave("groups", config.groups);
                if (client != null) client.setScreen(new GroupManagerScreen(parent));
            }).dimensions(width - PADDING - REMOVE_WIDTH, y, REMOVE_WIDTH, 20).build());
        }

        final ButtonWidget addBtn = ButtonWidget.builder(translatable("eventutils.config.groups.add"), button -> {
            config.groups.add(new PlayerGroup());
            config.setSave("groups", config.groups);
            client.setScreen(new GroupManagerScreen(parent));
        }).dimensions(width / 2 - BUTTON_WIDTH - 4, height - 32, BUTTON_WIDTH, 20).build();
        addDrawableChild(addBtn);

        addDrawableChild(ButtonWidget.builder(translatable("gui.done"), button -> goBack())
                .dimensions(width / 2 + 4, height - 32, BUTTON_WIDTH, 20).build());
    }

    private void goBack() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

}
