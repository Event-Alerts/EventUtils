package cc.aabss.eventutils.screen.group;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.minecraft.text.Text.translatable;


public class EditGroupScreen extends Screen {
    private static final int PADDING = 20;
    private static final int ROW = 24;
    private static final int FIELD_HEIGHT = 20;

    @Nullable private final Screen parent;
    @NotNull private final EventUtils mod;
    @NotNull private final UUID groupId;
    @NotNull private final Group groupCopy;
    private TextFieldWidget nameField;
    private TextFieldWidget playersField;
    private TextFieldWidget entitiesField;

    public EditGroupScreen(@Nullable Screen parent, @NotNull EventUtils mod, @NotNull UUID groupId, @NotNull Group group) {
        super(translatable("eventutils.config.groups.edit_title"));
        this.parent = parent;
        this.mod = mod;
        this.groupId = groupId;
        this.groupCopy = new Group(group);
    }

    @Override
    protected void init() {
        final int centerX = width / 2;
        final int fieldWidth = Math.min(320, width - PADDING * 4);
        final int fieldX = centerX - fieldWidth / 2;

        // Name (text)
        int y = 42;
        nameField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, FIELD_HEIGHT, translatable("eventutils.config.groups.name.hint"));
        nameField.setMaxLength(64);
        nameField.setText(groupCopy.getName());
        nameField.setPlaceholder(translatable("eventutils.config.groups.name.hint"));
        addDrawableChild(nameField);

        // Players (text)
        y += FIELD_HEIGHT + ROW;
        playersField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, FIELD_HEIGHT, translatable("eventutils.config.groups.players.hint"));
        playersField.setMaxLength(1024);
        playersField.setText(String.join(", ", groupCopy.getPlayers()));
        playersField.setPlaceholder(translatable("eventutils.config.groups.players.hint"));
        addDrawableChild(playersField);

        // Entities (text)
        y += FIELD_HEIGHT + ROW;
        entitiesField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, FIELD_HEIGHT, translatable("eventutils.config.groups.entities.hint"));
        entitiesField.setMaxLength(1024);
        entitiesField.setText(String.join(", ", groupCopy.getEntities()));
        entitiesField.setPlaceholder(translatable("eventutils.config.groups.entities.hint"));
        addDrawableChild(entitiesField);

        // Radius (slider)
        y += ROW;
        final Integer radius = groupCopy.getRadius();
        addDrawableChild(new SliderWidget(fieldX, y, fieldWidth, 20,
                getRadiusLabel(radius),
                radius == null ? 1.0 : (radius - 1) / (double) (Group.MAX_RADIUS - 1)) {

            @Override
            protected void updateMessage() {
                setMessage(getRadiusLabel(value >= 1.0 ? null : getRadiusFromSliderValue(value)));
            }

            @Override
            protected void applyValue() {
                groupCopy.setRadius(value >= 1.0 ? null : getRadiusFromSliderValue(value));
            }
        });

        // Player mode (button)
        y += ROW;
        addDrawableChild(ButtonWidget.builder(translatable("eventutils.config.groups.player_mode", translatable("eventutils.config.groups.mode." + groupCopy.getPlayerMode())), button -> {
            groupCopy.togglePlayerMode();
            button.setMessage(translatable("eventutils.config.groups.player_mode", translatable("eventutils.config.groups.mode." + groupCopy.getPlayerMode())));
        }).dimensions(centerX - 100, y, 200, 20).build());

        // Entity mode (button)
        y += ROW;
        addDrawableChild(ButtonWidget.builder(translatable("eventutils.config.groups.entity_mode", translatable("eventutils.config.groups.mode." + groupCopy.getEntityMode())), button -> {
            groupCopy.toggleEntityMode();
            button.setMessage(translatable("eventutils.config.groups.entity_mode", translatable("eventutils.config.groups.mode." + groupCopy.getEntityMode())));
        }).dimensions(centerX - 100, y, 200, 20).build());

        // NPC mode (button)
        y += ROW;
        addDrawableChild(ButtonWidget.builder(translatable("eventutils.config.groups.npc_mode", translatable("eventutils.config.groups.mode." + groupCopy.getNpcMode())), button -> {
            groupCopy.toggleNpcMode();
            button.setMessage(translatable("eventutils.config.groups.npc_mode", translatable("eventutils.config.groups.mode." + groupCopy.getNpcMode())));
        }).dimensions(centerX - 100, y, 200, 20).build());

        // Done (button)
        y += ROW + 8;
        addDrawableChild(ButtonWidget.builder(translatable("gui.done"), button -> saveAndClose())
                .dimensions(centerX - 60, y, 120, 20).build());
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);

        final int centerX = width / 2;
        final int labelX = centerX - 160;
        int y = 30;
        context.drawTextWithShadow(textRenderer, translatable("eventutils.config.groups.name.label"), labelX, y, 0xFFb19e70);
        y += FIELD_HEIGHT + ROW;
        context.drawTextWithShadow(textRenderer, translatable("eventutils.config.groups.players.label"), labelX, y, 0xFFb19e70);
        y += FIELD_HEIGHT + ROW;
        context.drawTextWithShadow(textRenderer, translatable("eventutils.config.groups.entities.label"), labelX, y, 0xFFb19e70);

        super.render(context, mouseX, mouseY, delta);
    }

    @NotNull
    private static Text getRadiusLabel(@Nullable Integer radius) {
        final Text radiusText = radius == null
                ? Text.translatable("eventutils.config.groups.radius.infinite")
                : Text.literal(String.valueOf(radius));
        return Text.translatable("eventutils.config.groups.radius.label", radiusText);
    }

    private static int getRadiusFromSliderValue(double value) {
        return 1 + (int) Math.round(value * (Group.MAX_RADIUS - 1));
    }

    private void saveAndClose() {
        // Check if group with NEW name already exists
        final String newName = nameField.getText();
        if (!newName.equals(groupCopy.getName()) && mod.config.getGroupByName(newName) != null) return;

        // Text fields
        groupCopy
                .setName(newName)
                .setPlayers(stringToSet(playersField.getText()))
                .setEntities(stringToSet(entitiesField.getText()));

        // Save
        mod.config.groups.put(groupId, groupCopy);
        mod.config.setSave("groups", mod.config.groups);
        if (client != null) client.setScreen(parent);
    }

    @NotNull
    private static Set<String> stringToSet(@NotNull String text) {
        text = text.trim().toLowerCase();
        if (text.isEmpty()) return Set.of();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
