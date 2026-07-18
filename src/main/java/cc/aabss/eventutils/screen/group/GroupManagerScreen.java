package cc.aabss.eventutils.screen.group;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

import static net.minecraft.text.Text.literal;
import static net.minecraft.text.Text.translatable;


public class GroupManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int PADDING = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int REMOVE_WIDTH = 50;

    @Nullable private final Screen parent;
    @NotNull private final EventUtils mod;

    public GroupManagerScreen(@Nullable Screen parent, @NotNull EventUtils mod) {
        super(translatable("eventutils.config.groups.manage_title"));
        this.parent = parent;
        this.mod = mod;
    }

    @Override
    protected void init() {
        final int listTop = 40;

        int i = 0;
        for (final Map.Entry<UUID, Group> entry : mod.config.groups.entrySet()) {
            final UUID id = entry.getKey();
            final Group group = entry.getValue();
            final int y = listTop + i * ROW_HEIGHT;
            if (y >= height - 60) break;
            i++;

            final ButtonWidget editBtn = ButtonWidget.builder(
                    literal(group.getName()).fillStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE0E0E0))),
                    button -> {
                        if (client != null) client.setScreen(new EditGroupScreen(this, mod, id, group));
                    }
            ).dimensions(PADDING, y, width - PADDING * 2 - REMOVE_WIDTH - 4, 20).build();
            addDrawableChild(editBtn);

            addDrawableChild(ButtonWidget.builder(literal("X").formatted(Formatting.RED), button -> {
                if (mod.groupManager.selectedGroup == id) mod.groupManager.selectedGroup = null;

                mod.config.groups.remove(id);
                mod.config.setSave("groups", mod.config.groups);
                if (client != null) client.setScreen(new GroupManagerScreen(parent, mod));
            }).dimensions(width - PADDING - REMOVE_WIDTH, y, REMOVE_WIDTH, 20).build());
        }

        final ButtonWidget addBtn = ButtonWidget.builder(translatable("eventutils.config.groups.add"), button -> {
            mod.config.groups.put(UUID.randomUUID(), new Group());
            mod.config.setSave("groups", mod.config.groups);
            if (client != null) client.setScreen(new GroupManagerScreen(parent, mod));
        }).dimensions(width / 2 - BUTTON_WIDTH - 4, height - 32, BUTTON_WIDTH, 20).build();
        addDrawableChild(addBtn);

        addDrawableChild(ButtonWidget.builder(translatable("gui.done"), button -> goBack())
                .dimensions(width / 2 + 4, height - 32, BUTTON_WIDTH, 20).build());
    }

    private void goBack() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

}
