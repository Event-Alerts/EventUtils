package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.sdk.EventUtility;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAFamousEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;


public class EventInfoScreen extends Screen {
    private static final int BOX_WIDTH = 350;
    private static final int BOX_HEIGHT = 280;

    /**
     * {@link EAEvent} or {@link EAFamousEvent}
     */
    @NotNull private final Object event;

    public EventInfoScreen(@NotNull Object event) {
        //? if >=1.21.11 {
        /*super(Text.translatable(EventUtils.MOD.keybindManager.eventInfoKey.getId()));
        *///?} else {
        super(Text.translatable(EventUtils.MOD.keybindManager.eventInfoKey.getTranslationKey()));
        //?}
        this.event = event;
    }

    @Override
    public void render(DrawContext drawContext, int i, int j, float f) {
        final int boxX = (width - BOX_WIDTH) / 2;
        final int boxY = (height - BOX_HEIGHT) / 2;
        final int startX = boxX + (BOX_WIDTH / 2);

        // Draw box
        drawContext.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0x88000000);

        // Build lines
        final List<String> lines = new ArrayList<>();
        if (event instanceof EAEvent eaEvent) {
            if (eaEvent.title != null) lines.add(eaEvent.title);
            if (eaEvent.host != null) lines.add("Host: " + eaEvent.host);
            if (eaEvent.server != null) lines.add("Partner Server: " + eaEvent.server);
            if (eaEvent.created != null) lines.add("Created: " + formatTime(eaEvent.created));
            if (eaEvent.time != null) lines.add("Time: " + formatTime(eaEvent.time));
            if (eaEvent.ip != null) lines.add("IP: " + eaEvent.ip);
            if (eaEvent.prize != null) lines.add("Prize: " + eaEvent.prize);

            // Version
            final StringBuilder version = new StringBuilder();
            if (eaEvent.platforms != null && !eaEvent.platforms.isEmpty()) version.append(EventUtility.PlatformUtility.toDisplayString(eaEvent.platforms));
            if (eaEvent.version != null) {
                if (!version.isEmpty()) version.append(" ");
                version.append(eaEvent.version);
            }
            if (!version.isEmpty()) lines.add("Version: " + version);

            // Roles
            if (eaEvent.rolesNamed != null && !eaEvent.rolesNamed.isEmpty()) lines.add("Roles: " + eaEvent.rolesNamed.stream()
                    .map(role -> role.displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));

            if (eaEvent.description != null) lines.add("Description: " + eaEvent.description);
            if (eaEvent.id != null) lines.add("ID: " + eaEvent.id);
        } else if (event instanceof EAFamousEvent famousEvent) {
            if (famousEvent.type != null) lines.add("Type: " + famousEvent.type);
            if (famousEvent.user != null) lines.add("User: " + famousEvent.user);
            if (famousEvent.message != null) lines.add("Message: " + famousEvent.message);
        } else {
            lines.add("Unknown event type: " + event.getClass().getName());
        }

        // Draw lines
        final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int startY = boxY + 5;
        for (final String line : lines) {
            // Split long lines into multiple lines
            if (textRenderer.getWidth(line) > BOX_WIDTH - 10) {
                final List<OrderedText> splitLines = textRenderer.wrapLines(StringVisitable.plain(line), BOX_WIDTH - 10);
                for (final OrderedText splitLine : splitLines) {
                    drawContext.drawCenteredTextWithShadow(textRenderer, splitLine, startX, startY, 0xFFFFFFFF);
                    startY += 12;
                }
                continue;
            }

            // Draw line
            drawContext.drawCenteredTextWithShadow(textRenderer, line, startX, startY, 0xFFFFFFFF);
            startY += 12;
        }
    }

    @NotNull
    private String formatTime(@NotNull Date date) {
        Duration duration = Duration.between(Instant.now(), date.toInstant());
        final boolean future = !duration.isNegative();
        duration = duration.abs();

        final long hours = duration.toHours();
        final long minutes = duration.toMinutes();
        final long seconds = duration.toSecondsPart();
        return (future ? "in " : "") + hours + "h " + minutes + "m " + seconds + "s" + (future ? "" : " ago");
    }
}
