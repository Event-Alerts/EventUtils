package cc.aabss.eventutils.config;

import cc.aabss.eventutils.EventUtils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.text.Text.translatable;


public class EditGroupScreen extends Screen {
    private static final int PADDING = 20;
    private static final int ROW = 24;
    private static final int FIELD_HEIGHT = 20;

    @Nullable private final Screen parent;
    private final int groupIndex;
    private TextFieldWidget nameField;
    private TextFieldWidget playersField;
    private boolean showNametags = true;
    private boolean hideListedPlayers = false;
    private boolean hideListedNpcs = false;

    public EditGroupScreen(@Nullable Screen parent, int groupIndex) {
        super(translatable("eventutils.config.groups.edit_title"));
        this.parent = parent;
        this.groupIndex = groupIndex;
    }

    @Override
    protected void init() {
        final EventConfig config = EventUtils.MOD.config;
        if (groupIndex < 0 || groupIndex >= config.groups.size()) {
            if (client != null) client.setScreen(parent);
            return;
        }
        final PlayerGroup group = config.groups.get(groupIndex);
        showNametags = group.isShowNametags();
        hideListedPlayers = group.isHideListedPlayers();
        hideListedNpcs = group.isHideListedNpcs();

        final int centerX = width / 2;
        final int fieldWidth = Math.min(320, width - PADDING * 4);
        int y = 40;

        nameField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, FIELD_HEIGHT, translatable("eventutils.config.groups.name_hint"));
        nameField.setMaxLength(64);
        nameField.setText(group.getName());
        nameField.setPlaceholder(translatable("eventutils.config.groups.name_hint"));
        addDrawableChild(nameField);

        y += ROW + 8;
        playersField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, FIELD_HEIGHT, translatable("eventutils.config.groups.players_hint"));
        playersField.setMaxLength(1024);
        playersField.setText(String.join(", ", group.getPlayers()));
        playersField.setPlaceholder(translatable("eventutils.config.groups.players_hint"));
        addDrawableChild(playersField);

        y += ROW + 16;
        addDrawableChild(ButtonWidget.builder(showNametags ? translatable("eventutils.config.groups.nametags_on") : translatable("eventutils.config.groups.nametags_off"), button -> {
            showNametags = !showNametags;
            button.setMessage(showNametags ? translatable("eventutils.config.groups.nametags_on") : translatable("eventutils.config.groups.nametags_off"));
        }).dimensions(centerX - 100, y, 200, 20).build());

        y += ROW;
        addDrawableChild(ButtonWidget.builder(hideListedPlayers ? translatable("eventutils.config.groups.players_mode_hide") : translatable("eventutils.config.groups.players_mode_reveal"), button -> {
            hideListedPlayers = !hideListedPlayers;
            button.setMessage(hideListedPlayers ? translatable("eventutils.config.groups.players_mode_hide") : translatable("eventutils.config.groups.players_mode_reveal"));
        }).dimensions(centerX - 100, y, 200, 20).build());

        y += ROW;
        addDrawableChild(ButtonWidget.builder(hideListedNpcs ? translatable("eventutils.config.groups.npcs_mode_hide") : translatable("eventutils.config.groups.npcs_mode_reveal"), button -> {
            hideListedNpcs = !hideListedNpcs;
            button.setMessage(hideListedNpcs ? translatable("eventutils.config.groups.npcs_mode_hide") : translatable("eventutils.config.groups.npcs_mode_reveal"));
        }).dimensions(centerX - 100, y, 200, 20).build());

        y += ROW + 8;
        addDrawableChild(ButtonWidget.builder(translatable("gui.done"), button -> saveAndClose())
                .dimensions(centerX - 60, y, 120, 20).build());
    }

    private void saveAndClose() {
        final EventConfig config = EventUtils.MOD.config;
        if (groupIndex < 0 || groupIndex >= config.groups.size()) {
            if (client != null) client.setScreen(parent);
            return;
        }
        final PlayerGroup group = config.groups.get(groupIndex);
        group.setName(nameField.getText().trim().isEmpty() ? "New Group" : nameField.getText().trim());
        final String playersText = playersField.getText().trim();
        final List<String> players = playersText.isEmpty()
                ? List.of()
                : Arrays.stream(playersText.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
        group.setPlayers(players);
        group.setShowNametags(showNametags);
        group.setHideListedPlayers(hideListedPlayers);
        group.setHideListedNpcs(hideListedNpcs);
        config.setSave("groups", config.groups);
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        final int centerX = width / 2;
        context.drawTextWithShadow(textRenderer, translatable("eventutils.config.groups.name_label"), centerX - 160, 44, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, translatable("eventutils.config.groups.players_label"), centerX - 160, 44 + ROW + 8, 0xA0A0A0);
        super.render(context, mouseX, mouseY, delta);
    }
}
