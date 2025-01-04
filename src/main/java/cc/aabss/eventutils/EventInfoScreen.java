package cc.aabss.eventutils;

import cc.aabss.eventutils.utility.StringUtility;
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
    protected void init() {
        super.init();
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
        unixTimestamp = unixTimestamp.replaceAll("\"", "");
        final Instant timestamp = Instant.ofEpochMilli(Long.parseLong(unixTimestamp));
        final Instant now = Instant.now();
        final long timestampSeconds = timestamp.getEpochSecond();
        final long nowSeconds = now.getEpochSecond();
        final long seconds = Duration.between(timestamp, now).getSeconds();

        // In the future
        if (timestampSeconds > nowSeconds) {
            if (seconds < 60) return "in " + seconds + " seconds";
            if (seconds < 3600) return "in " + (Math.ceil((double) seconds / 60)) + " minutes";
            if (seconds < 86400) return "in " + (Math.ceil((double) seconds / 3600)) + " hours";
            return "in " + (Math.ceil((double) seconds / 86400)) + " days";
        }

        // In the past
        if (timestampSeconds < nowSeconds) {
            if (seconds < 60) return seconds + " seconds ago";
            if (seconds < 3600) return (Math.ceil((double) seconds / 60)) + " minutes ago";
            if (seconds < 86400) return (Math.ceil((double) seconds / 3600)) + " hours ago";
            return (Math.ceil((double) seconds / 86400)) + " days ago";
        }

        // Just now
        return "just now";
    }

    @NotNull
    private String formatList(final Iterable<?> list){
        // Get stringList
        final List<String> stringlist = new ArrayList<>();
        list.forEach(o -> stringlist.add(o.toString()));
        Collections.sort(stringlist);
        final int size = stringlist.size();

        // Format
        if (size == 1) return stringlist.getFirst();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) builder.append(stringlist.get(i)).append(", ");
        return builder.substring(0, builder.length() - 2);
    }
}
