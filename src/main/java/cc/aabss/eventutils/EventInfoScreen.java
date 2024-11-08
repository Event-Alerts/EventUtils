package cc.aabss.eventutils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class EventInfoScreen extends Screen {

    public EventInfoScreen(JsonObject json) {
        super(Text.translatable("key.eventutils.eventinfo"));
        this.json = json;
    }

    private final JsonObject json;

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext drawContext, int i, int j, float f) {
        int boxX = (this.width-200) / 2;
        int boxY = (this.height-300) / 2;
        int startX = boxX+10;
        int startY = boxY+10;

        drawContext.fill(boxX, boxY, boxX+200, boxY+300, 0x88000000);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String label = entry.getKey() + ": ";
            JsonElement jsonValue = entry.getValue();
            String value = jsonValue.toString();
            if (jsonValue.isJsonArray()) {
                value = formatList(jsonValue.getAsJsonArray());
            }
            if (Objects.equals(entry.getKey(), "time")) {
                value = formatTime(entry.getValue().toString());
            }
            drawContext.drawCenteredTextWithShadow(textRenderer, label+value, startX, startY, 0xFFFFFF);
            startY = startY+12;
        }

        super.render(drawContext, i, j, f);
    }

    private String formatTime(String unixTimestamp) {
        Instant timestamp = Instant.ofEpochSecond(Long.parseLong(unixTimestamp));
        Instant now = Instant.now();
        Duration duration = Duration.between(timestamp, now);

        long seconds = duration.getSeconds();

        if (timestamp.getEpochSecond() > now.getEpochSecond()){
            if (seconds < 60) {
                return "in " +seconds+(seconds == 1 ? " second" : " seconds");
            } else if (seconds < 3600) {
                double minutes = Math.ceil((double) (seconds/60));
                return "in " +minutes+(minutes == 1 ? " minute" : " minutes");
            } else {
                double hours = Math.ceil((double) (seconds / 3600));
                return "in " +hours+(hours == 1 ? " hour" : " hours");
            }
        } else if (timestamp.getEpochSecond() < now.getEpochSecond()) {
            if (seconds < 60) {
                return seconds + (seconds == 0 ? " second ago" : "seconds ago");
            } else if (seconds < 3600) {
                double minutes = Math.ceil((double) (seconds/60));
                return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            } else {
                double hours = Math.ceil((double) (seconds / 3600));
                return hours + (hours == 1 ? " hour ago" : " hours ago");
            }
        } else {
            return "just now";
        }
    }

    private String formatList(Iterable<?> list){
        List<String> stringlist = new ArrayList<>();
        list.forEach(o -> stringlist.add(o.toString()));
        Collections.sort(stringlist);
        StringBuilder builder = new StringBuilder();
        int i = 0;
        if (stringlist.size() == 1){
            return stringlist.getFirst();
        }
        for (Object obj : stringlist){
            if (i == stringlist.size() - 1) {
                builder.append(obj);
            } else if (i == stringlist.size() - 2) {
                builder.append(obj).append(" and ");
            } else {
                builder.append(obj).append(", ");
            }
            i++;
        }
        return builder.toString();
    }
}
