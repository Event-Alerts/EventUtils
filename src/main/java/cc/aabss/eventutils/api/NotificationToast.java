package cc.aabss.eventutils.api;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NotificationToast implements Toast {
    private static final Identifier TEXTURE = new Identifier("eventutils:toast/notification");
    private final NotificationToast.Type type;
    private final Text title;
    private final List<OrderedText> lines;
    private long startTime;
    private boolean justUpdated;
    private final int width;

    public NotificationToast(NotificationToast.Type type, Text title, @Nullable Text description) {
        this(type, title, NotificationToast.getTextAsList(description), Math.max(160, 30 + Math.max(MinecraftClient.getInstance().textRenderer.getWidth(title), description == null ? 0 : MinecraftClient.getInstance().textRenderer.getWidth(description))));
    }

    private NotificationToast(NotificationToast.Type type, Text title, List<OrderedText> lines, int width) {
        this.type = type;
        this.title = title;
        this.lines = lines;
        this.width = width;
    }

    private static ImmutableList<OrderedText> getTextAsList(@Nullable Text text) {
        return text == null ? ImmutableList.of() : ImmutableList.of(text.asOrderedText());
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return 20 + Math.max(this.lines.size(), 1) * 12;
    }

    @Override
    public Toast.Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        int j;
        int i;
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }
        if ((i = this.getWidth()) == 160 && this.lines.size() <= 1) {
            context.drawGuiTexture(TEXTURE, 0, 0, i, this.getHeight());
        } else {
            j = this.getHeight();
            int l = Math.min(4, j - 28);
            this.drawPart(context, i, 0, 0, 28);
            for (int m = 28; m < j - l; m += 10) {
                this.drawPart(context, i, 16, m, Math.min(16, j - m - l));
            }
            this.drawPart(context, i, 32 - l, j - l, l);
        }
        if (this.lines.isEmpty()) {
            context.drawText(manager.getClient().textRenderer, this.title, 24, 12, Colors.YELLOW, false);
        } else {
            context.drawText(manager.getClient().textRenderer, this.title, 24, 7, Colors.YELLOW, false);
            for (j = 0; j < this.lines.size(); ++j) {
                context.drawText(manager.getClient().textRenderer, this.lines.get(j), 24, 18 + j * 12, -1, false);
            }
        }
        double d = (double)this.type.displayDuration * manager.getNotificationDisplayTimeMultiplier();
        long n = startTime - this.startTime;
        return (double)n < d ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    private void drawPart(DrawContext context, int i, int j, int k, int l) {
        int m = j == 0 ? 20 : 5;
        int n = Math.min(60, i - m);
        Identifier identifier = TEXTURE;
        context.drawGuiTexture(identifier, 160, 32, 0, j, 0, k, m, l);
        for (int o = m; o < i - n; o += 64) {
            context.drawGuiTexture(identifier, 160, 32, 32, j, o, k, Math.min(64, i - o - n), l);
        }
        context.drawGuiTexture(identifier, 160, 32, 160 - n, j, i - n, k, n, l);
    }

    @Override
    public NotificationToast.Type getType() {
        return this.type;
    }

    public static void add(ToastManager manager, NotificationToast.Type type, Text title, @Nullable Text description) {
        manager.add(new NotificationToast(type, title, description));
    }

    public static void addFamousEvent(){
        MutableText event = text("New Famous Event!").formatted(Formatting.AQUA);
        templateEvent(event, "famous");
    }

    public static void addPotentialFamousEvent(){
        MutableText event = text("New Potential Famous Event!").formatted(Formatting.DARK_AQUA);
        templateEvent(event, "potential");
    }

    public static void addMoneyEvent(){
        MutableText event = text("New Money Event!").formatted(Formatting.GREEN);
        templateEvent(event, "money");
    }

    public static void addPartnerEvent(){
        MutableText event = text("New Partner Event!").formatted(Formatting.LIGHT_PURPLE);
        templateEvent(event, "partner");
    }

    public static void addFunEvent(){
        MutableText event = text("New Fun Event!").formatted(Formatting.RED);
        templateEvent(event, "fun");
    }

    public static void addHousingEvent(){
        MutableText event = text("New Housing Event!").formatted(Formatting.GOLD);
        templateEvent(event, "housing");
    }

    public static void addCommunityEvent(){
        MutableText event = text("New Community Event!").formatted(Formatting.DARK_GRAY);
        templateEvent(event, "community");
    }

    public static void addCivilizationEvent(){
        MutableText event = text("New Civilization Event!").formatted(Formatting.BLUE);
        templateEvent(event, "civilization");
    }

    // --

    private static void templateEvent(MutableText text, String eventType){
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            MutableText type = text("Type ").formatted(Formatting.WHITE);
            MutableText command = text("/eventtp " + eventType).formatted(Formatting.YELLOW);
            MutableText teleport = text(" to teleport!").formatted(Formatting.WHITE);
            client.player.playSound(SoundEvent.of(new Identifier("eventutils:alert")), 1 ,1);
            add(client.getToastManager(), NotificationToast.Type.DEFAULT, text, append(type, command, teleport));
        }
    }

    private static MutableText text(String s){
        return MutableText.of(new PlainTextContent.Literal(s));
    }

    private static MutableText append(MutableText... texts){
        MutableText mutableText = text("");
        for (MutableText text : texts){
            mutableText.append(text);
        }
        return mutableText;
    }

    @Environment(value= EnvType.CLIENT)
    public static class Type {
        public static final NotificationToast.Type DEFAULT = new NotificationToast.Type(10000L);
        final long displayDuration;
        public Type(long displayDuration) {
            this.displayDuration = displayDuration;
        }
    }
}
