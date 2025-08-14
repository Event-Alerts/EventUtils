package cc.aabss.eventutils;

import cc.aabss.eventutils.utility.StringUtility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;


public class EventInfoScreen extends Screen {
    private static final int BOX_WIDTH = 350;
    private static final int BOX_HEIGHT = 280;

    @NotNull private final JsonObject json;

    public EventInfoScreen(@NotNull JsonObject json) {
        super(Text.translatable("key.eventutils.eventinfo"));
        this.json = json;
    }

    @Override
    public void render(DrawContext drawContext, int i, int j, float f) {
        final int boxX = (width - BOX_WIDTH) / 2;
        final int boxY = (height - BOX_HEIGHT) / 2;
        final int startX = boxX + (BOX_WIDTH / 2);
        int startY = boxY + 5;

        drawContext.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0x88000000);
        final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            // Get text
            final String key = entry.getKey();
            final JsonElement jsonValue = entry.getValue();

            // Get value as string
            String value;
            if (jsonValue.isJsonArray()) {
                value = formatList(jsonValue.getAsJsonArray());
            } else if (jsonValue.isJsonPrimitive()) {
                value = jsonValue.getAsString();
            } else {
                value = jsonValue.toString();
            }

            // Format time
            if (key.equals("time") || key.equals("created")) value = formatTime(value);

            // Draw
            drawContext.drawCenteredTextWithShadow(textRenderer, StringUtility.capitalize(key) + ": " + value, startX, startY, 0xFFFFFF);
            startY += 12;
        }
    }

    @NotNull
    private String formatTime(@NotNull String unixTimestamp) {
        // Get timestamps
        unixTimestamp = unixTimestamp.replaceAll("\"", "");
        final Instant timestamp;
        try {
            timestamp = Instant.ofEpochMilli(Long.parseLong(unixTimestamp));
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to parse timestamp: {}", unixTimestamp, e);
            return unixTimestamp; // Not a valid timestamp
        }
        final Instant now = Instant.now();

        // Get seconds
        final long timestampSeconds = timestamp.getEpochSecond();
        final long nowSeconds = now.getEpochSecond();
        if (timestampSeconds == nowSeconds) return "now";

        final StringBuilder builder = new StringBuilder();
        if (timestampSeconds > nowSeconds) builder.append("in "); // Future

        final long seconds = Duration.between(timestamp, now).getSeconds();
        if (seconds < 60) {
            builder.append(seconds).append(" seconds");
        } else if (seconds < 3600) {
            builder.append((Math.ceil((double) seconds / 60))).append(" minutes");
        } else if (seconds < 86400) {
            builder.append((Math.ceil((double) seconds / 3600))).append(" hours");
        } else {
            builder.append((Math.ceil((double) seconds / 86400))).append(" days");
        }

        if (timestampSeconds < nowSeconds) builder.append(" ago"); // Past
        return builder.toString();
    }

    @NotNull
    private String formatList(@NotNull JsonArray list) {
        // Check size
        final int size = list.size();
        if (size == 0) return "";
        if (size == 1) return list.get(0).toString();

        // Format
        final StringBuilder builder = new StringBuilder();
        for (final JsonElement element : list) builder.append(element).append(", ");
        return builder.substring(0, builder.length() - 2);
    }
}
