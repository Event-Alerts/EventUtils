package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.NotificationToast;
import gg.eventalerts.sdk.object.EAEvent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ServerStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.manipulation.DurationParser;
import xyz.srnyx.javautilities.manipulation.Mapper;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;


public class EventBuilder extends ScreenWithParent<FlowLayout> {
    @NotNull private String title = Defaults.TITLE;
    @NotNull private String time = Defaults.TIME;
    @NotNull private String ip = Defaults.ip();
    @NotNull private final Set<EAEvent.Platform> platforms = Defaults.platforms();
    @NotNull private String version = Defaults.version();
    @Nullable private String description = Defaults.DESCRIPTION;
    @Nullable private String prize = Defaults.PRIZE;
    @Nullable private Integer maxPlayers = Defaults.maxPlayers();

    public EventBuilder(@Nullable Screen parent) {
        super(parent);
    }

    public static class Defaults {
        @NotNull public static final String TITLE =  Minecraft.getInstance().getUser().getName() + "'s Event";
        @NotNull public static final String TIME = "15m";
        @Nullable private static final String IP = "play.eventalerts.gg";
        @Nullable public static final String DESCRIPTION = null;
        @NotNull private static final Set<EAEvent.Platform> PLATFORMS = Set.of(EAEvent.Platform.JAVA);
        @Nullable public static final String PRIZE = null;
        @Nullable public static final Integer MAX_PLAYERS = null;

        @NotNull
        public static String ip() {
            final ServerData server = Minecraft.getInstance().getCurrentServer();
            return server != null ? server.ip : IP;
        }

        @NotNull
        public static Set<EAEvent.Platform> platforms() {
            return new HashSet<>(PLATFORMS);
        }

        @NotNull
        public static String version() {
            return Objects.requireNonNullElse(EventUtils.MC_VERSION, "26.2");
        }

        @Nullable
        public static String description() {
            final ServerData server = Minecraft.getInstance().getCurrentServer();
            return server != null ? server.motd.getString() : DESCRIPTION;
        }

        @Nullable
        public static Integer maxPlayers() {
            final ServerData server = Minecraft.getInstance().getCurrentServer();
            if (server == null) return Defaults.MAX_PLAYERS;
            final ServerStatus.Players players = server.players;
            return players != null ? Integer.valueOf(players.max()) : Defaults.MAX_PLAYERS;
        }
    }

    private void post() {
        // Get Date time
        final Duration duration = DurationParser.parse(time).orElse(null);
        if (duration == null) {
            NotificationToast.show(translatable("eventutils.event_builder.error.invalid_time.title"), translatable("eventutils.event_builder.error.invalid_time.description", time));
            return;
        }
        final Date dateTime = new Date(System.currentTimeMillis() + duration.toMillis());

        //TODO Local checks

        // Build
        final EAEvent event = new EAEvent();
        // Constants
        event.type = EAEvent.Type.COMMUNITY;
        event.custom = false;
        // Values
        event.title = this.title;
        event.description = this.description;
        event.time = dateTime;
        event.ip = this.ip;
        event.platforms = this.platforms;
        event.version = this.version;
        event.prize = this.prize;
        event.maxPlayers = this.maxPlayers;

        // Post
        NotificationToast.show(translatable("eventutils.event_builder.actions.post.toast.title"), translatable("eventutils.event_builder.actions.post.toast.description"));
        EventUtils.MOD.http.events.postOne(event).queue(
                success -> {
                    // Screen
                    Minecraft.getInstance().execute(this::onClose);
                    // Toast
                    NotificationToast.show(translatable("eventutils.event_builder.success.title"), translatable("eventutils.event_builder.success.description"));
                    // Stat
                    EventUtils.MOD.stats.eventsPosted.incrementAndGet();
                },
                failure -> {
                    // Log
                    EventUtils.LOGGER.error("Failed to post event", failure);
                    // Toast
                    NotificationToast.show(translatable("eventutils.event_builder.error.post.title"), Component.literal(failure.getMessage()));
                });
    }

    @Override @NotNull
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(@NotNull FlowLayout root) {
        root.gap(4);
        root
                .surface(Surface.VANILLA_TRANSLUCENT)
                .padding(Insets.of(20))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        root.child(Components.label(translatable("eventutils.event_builder.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .shadow(true)
                .margins(Insets.bottom(8)));

        root.child(stringRow("title", true, title, value -> title = value));
        root.child(stringRow("time", true, time, value -> time = value));
        root.child(stringRow("ip", true, ip, value -> ip = value));
        root.child(stringRow("version", true, version, value -> version = value));
        root.child(textAreaRow("description", false, description, value -> description = value));
        root.child(stringRow("prize", false, prize, value -> prize = value));
        root.child(platformsRow());
        root.child(integerRow("max_players", false, maxPlayers, value -> maxPlayers = value));

        final FlowLayout actions = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        actions.gap(6);
        actions.margins(Insets.top(12));
        actions.child(Components.button(translatable("eventutils.event_builder.actions.post.button").withStyle(ChatFormatting.GREEN), button -> post())
                .horizontalSizing(Sizing.fixed(160)));
        actions.child(Components.button(translatable("gui.cancel"), button -> onClose())
                .horizontalSizing(Sizing.fixed(90)));
        root.child(actions);
    }

    @NotNull
    private FlowLayout stringRow(@NotNull String key, boolean required, @Nullable String value, @NotNull Consumer<String> setter) {
        final TextBoxComponent box = Components.textBox(Sizing.expand(), value == null ? "" : value);
        box.onChanged().subscribe(setter::accept);
        return row(key, required, box);
    }

    @NotNull
    private FlowLayout integerRow(@NotNull String key, boolean required, @Nullable Number value, @NotNull Consumer<Integer> setter) {
        final TextBoxComponent box = Components.textBox(Sizing.expand(), value == null ? "" : value.toString());
        box.setFilter(text -> text.isEmpty() || (text.length() <= 9 && text.chars().allMatch(Character::isDigit)));
        box.onChanged().subscribe(newValue -> setter.accept(Mapper.toInt(newValue).orElse(null)));
        return row(key, required, box);
    }

    @NotNull
    private FlowLayout textAreaRow(@NotNull String key, boolean required, @Nullable String value, @NotNull Consumer<String> setter) {
        final TextAreaComponent area = Components.textArea(Sizing.expand(), Sizing.fixed(60), value == null ? "" : value);
        area.onChanged().subscribe(setter::accept);
        return row(key, required, area);
    }

    @NotNull
    private FlowLayout platformsRow() {
        final FlowLayout checkboxes = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        checkboxes.gap(12);
        checkboxes.verticalAlignment(VerticalAlignment.CENTER);
        for (final EAEvent.Platform platform : EAEvent.Platform.values()) checkboxes.child(Components.checkbox(literal(platform.displayName))
                .checked(platforms.contains(platform))
                .onChanged(checked -> {
                    if (checked) {
                        platforms.add(platform);
                    } else {
                        platforms.remove(platform);
                    }
                }));
        return row("platforms", false, checkboxes);
    }

    @NotNull
    private FlowLayout row(@NotNull String key, boolean required, @NotNull io.wispforest.owo.ui.core.Component input) {
        final FlowLayout row = Containers.horizontalFlow(Sizing.fixed(420), Sizing.content());
        row.gap(8);
        row.verticalAlignment(VerticalAlignment.CENTER);

        final MutableComponent labelText = translatable("eventutils.event_builder.values." + key + ".label");
        if (required) labelText.withStyle(ChatFormatting.BOLD);

        final LabelComponent label = Components.label(labelText);
        label.horizontalTextAlignment(HorizontalAlignment.RIGHT);
        label.horizontalSizing(Sizing.fixed(75));

        row.child(label);
        row.child(input);
        return row;
    }
}
