package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.NotificationToast;
import cc.aabss.eventutils.sdk.EnrichedPlayer;
import cc.aabss.eventutils.versioning.VersionedClient;
import cc.aabss.eventutils.versioning.VersionedIdentifier;
import gg.eventalerts.sdk.object.EAEvent;
import gg.eventalerts.sdk.object.EAPartnerServer;
import gg.eventalerts.sdk.object.EAPlayer;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ServerStatus;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.srnyx.javautilities.MapGenerator;
import xyz.srnyx.javautilities.manipulation.DurationParser;
import xyz.srnyx.javautilities.manipulation.Mapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;


public class EventBuilder extends ScreenWithParent<FlowLayout> {
    public static final int MAX_TITLE_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;
    public static final int MAX_TIME_LENGTH = 1007;
    public static final int MAX_IP_LENGTH = 256;
    public static final int MAX_VERSION_LENGTH = 256;
    public static final int MAX_PRIZE_LENGTH = 256;
    @NotNull private static final List<EAEvent.PingRole> TOGGLEABLE_ROLES = Arrays.stream(EAEvent.PingRole.values())
            .filter(role -> role.partnerToggleable)
            .toList();
    @NotNull private static final ObjectId COMMUNITY_EVENT_SENTINEL = new ObjectId("000000000000000000000000");

    @NotNull private Map<ObjectId, EAPartnerServer> partnerServers = new LinkedHashMap<>();

    // Values
    private boolean custom = Defaults.CUSTOM;
    @Nullable private ObjectId partnerServer;
    @NotNull private Set<EAEvent.PingRole> roles = Defaults.pingRoles();
    @NotNull private String title = Defaults.TITLE;
    @NotNull private String time = Defaults.TIME;
    @NotNull private String ip = Defaults.ip();
    @NotNull private Set<EAEvent.Platform> platforms = Defaults.platforms();
    @NotNull private String version = Defaults.version();
    @NotNull private String description = Defaults.DESCRIPTION;
    @NotNull private String prize = Defaults.PRIZE;
    @Nullable private Integer maxPlayers = Defaults.maxPlayers();

    public EventBuilder(@Nullable Screen parent) {
        super(parent);
        confirmClose(
                translatable("eventutils.event_builder.confirm_close.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                translatable("eventutils.event_builder.confirm_close.description"));
    }

    private EventBuilder(@NotNull EventBuilder builder) {
        this(builder.parent);

        this.partnerServers = new LinkedHashMap<>(builder.partnerServers);

        // Values
        this.custom = builder.custom;
        this.partnerServer = builder.partnerServer;
        this.roles = new HashSet<>(builder.roles);
        this.title = builder.title;
        this.time = builder.time;
        this.ip = builder.ip;
        this.platforms = new HashSet<>(builder.platforms);
        this.version = builder.version;
        this.description = builder.description;
        this.prize = builder.prize;
        this.maxPlayers = builder.maxPlayers;

        // Checks
        if (custom && (EventUtils.MOD.authManager.player == null || EventUtils.MOD.authManager.player.subscription == null) && partnerServer == null) {
            this.custom = false;
        }
    }

    public void open() {
        final EAPlayer player = EventUtils.MOD.authManager.player;
        if (player == null || player.discord == null) return;

        EventUtils.MOD.http.partnerServers.retrieveAll(MapGenerator.HASH_MAP.mapOf(
                "enabled", null,
                "representatives", Set.of(player.discord)
        )).queue(
                partnerServers -> {
                    // partnerServers
                    this.partnerServers = new LinkedHashMap<>();
                    for (final EAPartnerServer partnerServer : partnerServers) {
                        if (partnerServer.id != null) this.partnerServers.put(partnerServer.id, partnerServer);
                    }

                    // No default preset, open immediately
                    if (player.defaultPreset == null) {
                        setScreen();
                        return;
                    }

                    // Default preset, retrieve -> apply -> open
                    EventUtils.MOD.http.eventPresets.retrieveOneById(player.defaultPreset).queue(
                            preset -> {
                                // Apply preset
                                if (preset.data != null) {
                                    this.custom = false;
                                    if (preset.data.title != null) this.title = preset.data.title;
                                    if (preset.data.time != null) this.time = preset.data.time;
                                    if (preset.data.ip != null) this.ip = preset.data.ip;
                                    if (preset.data.platforms != null) this.platforms = new HashSet<>(preset.data.platforms);
                                    if (preset.data.version != null) this.version = preset.data.version;
                                    if (preset.data.description != null) this.description = preset.data.description;
                                    if (preset.data.prize != null) this.prize = preset.data.prize;
                                    if (preset.data.maxPlayers != null) this.maxPlayers = preset.data.maxPlayers;
                                }

                                // Open
                                setScreen();
                            },
                            failure -> {
                                EventUtils.LOGGER.error("Failed to retrieve default preset", failure);
                                error("preset", Component.literal(failure.getMessage()));
                                setScreen(); // Still open, just without default preset
                            });
                },
                failure -> {
                    EventUtils.LOGGER.error("Failed to retrieve partner servers", failure);
                    error("partner_servers", Component.literal(failure.getMessage()));
                });
    }

    private void setScreen() {
        final Minecraft client = Minecraft.getInstance();
        client.execute(() -> new VersionedClient(client).setScreen(this));
    }

    private void rebuild() {
        new VersionedClient(Minecraft.getInstance()).setScreen(new EventBuilder(this));
    }

    private void error(@NotNull String key, @NotNull Object... args) {
        NotificationToast.show(
                translatable("eventutils.event_builder.error." + key + ".title").withStyle(ChatFormatting.RED),
                translatable("eventutils.event_builder.error." + key + ".description", args));
        setScreen();
    }

    private boolean canPost() {
        // Check if host is linked
        final EnrichedPlayer player = EventUtils.MOD.authManager.player;
        if (player == null || player.discord == null || player.minecraft == null) {
            error("not_logged_in");
            return false;
        }

        final EAEvent.Type type = partnerServer != null ? EAEvent.Type.PARTNER : EAEvent.Type.COMMUNITY;
        final Set<String> missingRequiredFields = new HashSet<>();

        if (custom) {
            // Custom

            // Required fields
            if (description.isBlank()) missingRequiredFields.add("description");
        } else {
            // Builder

            // Required fields
            if (title.isBlank()) missingRequiredFields.add("title");
            if (time.isBlank()) missingRequiredFields.add("time");
            if (ip.isBlank()) missingRequiredFields.add("ip");
            if (version.isBlank()) missingRequiredFields.add("version");

            // Get time as Duration
            final Duration duration = DurationParser.parse(time).orElse(null);
            if (duration == null) {
                error("invalid_time", time);
                return false;
            }
            // Check if duration is more than 1 day
            if (duration.toDays() > 1) {
                error("time_too_long", time);
                return false;
            }

            // Custom COMMUNITY required premium
            if (type == EAEvent.Type.COMMUNITY && player.getEffectiveSubscriptionTier() == null) {
                error("premium_required", translatable("eventutils.event_builder.error.premium_required.custom_community"));
                return false;
            }
        }

        // Missing required field
        if (!missingRequiredFields.isEmpty()) {
            final MutableComponent missingFields = Component.empty();
            for (final String field : missingRequiredFields) {
                if (!missingFields.getSiblings().isEmpty()) missingFields.append(", ");
                missingFields.append(translatable("eventutils.event_builder.values." + field + ".label"));
            }
            error("missing_required", missingFields);
            return false;
        }

        return true;
    }

    private void post() {
        if (!canPost()) return;
        final EAEvent event = new EAEvent();

        // Common
        event.type = this.partnerServer != null ? EAEvent.Type.PARTNER : EAEvent.Type.COMMUNITY;
        event.custom = this.custom;
        event.server = this.partnerServer;
        event.description = this.description;

        // Partner-only
        if (event.type == EAEvent.Type.PARTNER) event.rolesNamed = this.roles;

        if (!custom) {
            // Builder

            // Get time as Duration
            final Duration duration = DurationParser.parse(time).orElse(null);
            if (duration == null) {
                error("invalid_time", time);
                return;
            }
            // Check if duration is more than 1 day
            if (duration.toDays() > 1) {
                error("time_too_long", time);
                return;
            }

            event.title = this.title;
            event.time = new Date(System.currentTimeMillis() + duration.toMillis());
            event.ip = this.ip;
            event.platforms = this.platforms;
            event.version = this.version;
            event.prize = this.prize;
            event.maxPlayers = this.maxPlayers;
        }

        // Post
        NotificationToast.show(translatable("eventutils.event_builder.actions.post.toast.title"), translatable("eventutils.event_builder.actions.post.toast.description"));
        EventUtils.MOD.http.events.postOne(event).queue(
                success -> {
                    // Screen
                    Minecraft.getInstance().execute(this::closeWithoutConfirmation);
                    // Toast
                    NotificationToast.show(translatable("eventutils.event_builder.success.title"), translatable("eventutils.event_builder.success.description"));
                    // Stat
                    EventUtils.MOD.stats.eventsPosted.incrementAndGet();
                },
                failure -> {
                    EventUtils.LOGGER.error("Failed to post event", failure);
                    error("post", Component.literal(failure.getMessage()));
                });
    }

    @Override
    protected void build(@NotNull FlowLayout root) {
        final EnrichedPlayer player = EventUtils.MOD.authManager.player;

        root.gap(4);
        root
                .surface(Surface.VANILLA_TRANSLUCENT)
                .padding(Insets.of(10))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        final FlowLayout fields = Containers.verticalFlow(Sizing.content(), Sizing.content());
        fields.padding(Insets.right(5));
        fields.gap(4);

        // Partner Server
        if (!partnerServers.isEmpty()) {
            // Build Partner Server option IDs
            final List<ObjectId> partnerServerIds = new ArrayList<>(partnerServers.keySet());
            partnerServerIds.addFirst(COMMUNITY_EVENT_SENTINEL); // Add community event option

            fields.child(dropdownRow(
                    "partner_server", true, partnerServerIds,
                    Objects.requireNonNullElse(partnerServer, COMMUNITY_EVENT_SENTINEL), id -> {
                        final ObjectId oldPartnerServer = this.partnerServer;
                        partnerServer = id == COMMUNITY_EVENT_SENTINEL ? null : id;

                        // Rebuild if switching between community and partner (for ping roles)
                        if ((oldPartnerServer == null && partnerServer != null) || (oldPartnerServer != null && partnerServer == null)) {
                            rebuild();
                        }
                    },
                    id -> {
                        if (id == null || id == COMMUNITY_EVENT_SENTINEL) {
                            return translatable("eventutils.event_builder.values.partner_server.community").withStyle(ChatFormatting.GRAY);
                        }
                        final EAPartnerServer server = partnerServers.get(id);
                        return literal(server != null && server.name != null ? server.name : id.toHexString());
                    }));
        }
        // Roles (Partner only) TODO role conditions
        if (partnerServer != null) fields.child(checkBoxRow(
                "roles", !custom,
                TOGGLEABLE_ROLES, roles,
                pingRole -> literal(pingRole.displayName),
                pingRole -> "textures/event_builder/role/" + pingRole.name().toLowerCase() + ".png"));

        if (!custom) {
            fields.child(stringRow("title", true, MAX_TITLE_LENGTH, title, value -> title = value));
            fields.child(stringRow("time", true, MAX_TIME_LENGTH, time, value -> time = value));
            fields.child(stringRow("ip", true, MAX_IP_LENGTH, ip, value -> ip = value));
            fields.child(stringRow("version", true, MAX_VERSION_LENGTH, version, value -> version = value));
        }
        fields.child(textAreaRow("description", custom, MAX_DESCRIPTION_LENGTH, custom, description, value -> description = value));
        if (!custom) {
            fields.child(stringRow("prize", false, MAX_PRIZE_LENGTH, prize, value -> prize = value));
            fields.child(checkBoxRow(
                    "platforms", false,
                    List.of(EAEvent.Platform.values()), platforms,
                    platform -> literal(platform.displayName),
                    platform -> "textures/event_builder/platform/" + platform.name().toLowerCase() + ".png"));
            //TODO does this allow negatives?
            fields.child(integerRow("max_players", false, maxPlayers, value -> maxPlayers = value));
        }
        root.child(Containers.verticalScroll(Sizing.content(), Sizing.expand(), fields));

        final FlowLayout actions = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        actions.gap(6);
        actions.margins(Insets.top(12));
        final String customKey = custom ? "custom" : "builder";
        if ((player != null && player.getEffectiveSubscriptionTier() != null) || partnerServer != null) {
            actions.child(Components.button(translatable("eventutils.event_builder.actions.custom." + customKey).withStyle(ChatFormatting.AQUA), button -> {
                custom = !custom;
                rebuild();
            }).horizontalSizing(Sizing.fixed(160)));
        }
        actions.child(Components.button(translatable("eventutils.event_builder.actions.post.label").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),  button -> {
            if (!canPost()) return;
            final VersionedClient vClient = new VersionedClient(Minecraft.getInstance());
            vClient.setScreen(new ConfirmScreen(result -> {
                if (result) {
                    post();
                } else {
                    vClient.setScreen(this);
                }
            }, translatable("eventutils.event_builder.actions.post.confirm.title").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), translatable("eventutils.event_builder.actions.post.confirm.description")));
        })
                .horizontalSizing(Sizing.fixed(160)));
        actions.child(Components.button(translatable("gui.cancel").withStyle(ChatFormatting.RED), button -> onClose())
                .horizontalSizing(Sizing.fixed(90)));
        root.child(actions);
    }

    @NotNull
    private FlowLayout stringRow(@NotNull String key, boolean required, int maxLength, @Nullable String value, @NotNull Consumer<String> setter) {
        final String initial = value == null ? "" : value;
        final TextBoxComponent box = Components.textBox(Sizing.expand(), initial);
        final FlowLayout wrapper = inputWrapper(box, required && initial.isBlank());
        box.onChanged().subscribe(newValue -> {
            String result = newValue;
            if (newValue.length() > maxLength) {
                result = newValue.substring(0, maxLength);
                box.text(result);
            }
            setter.accept(result);
            markInvalid(wrapper, required && result.isBlank());
        });
        return row(key, required, wrapper);
    }

    @NotNull
    private FlowLayout integerRow(@NotNull String key, boolean required, @Nullable Number value, @NotNull Consumer<Integer> setter) {
        final String initial = value == null ? "" : value.toString();
        final TextBoxComponent box = Components.textBox(Sizing.expand(), initial);
        final FlowLayout wrapper = inputWrapper(box, required && initial.isBlank());
        box.setFilter(text -> text.isEmpty() || (text.length() <= 9 && text.chars().allMatch(Character::isDigit)));
        box.onChanged().subscribe(newValue -> {
            setter.accept(Mapper.toInt(newValue).orElse(null));
            markInvalid(wrapper, required && newValue.isBlank());
        });
        return row(key, required, box);
    }

    @NotNull
    private FlowLayout textAreaRow(@NotNull String key, boolean required, int maxLength, boolean big, @Nullable String value, @NotNull Consumer<String> setter) {
        final String initial = value == null ? "" : value;
        final TextAreaComponent area = Components.textArea(Sizing.expand(), Sizing.fixed(big ? 200 : 60), initial);
        final FlowLayout wrapper = inputWrapper(area, required && initial.isBlank());
        area.margins(Insets.of(1));
        area.onChanged().subscribe(newValue -> {
            String result = newValue;
            if (newValue.length() > maxLength) {
                result = newValue.substring(0, maxLength);
                area.text(result);
            }
            setter.accept(newValue);
            markInvalid(wrapper, required && result.isBlank());
        });
        return row(key, required, wrapper);
    }

    @NotNull
    private <T> FlowLayout dropdownRow(
            @NotNull String key, boolean required, @NotNull Collection<T> values,
            @Nullable T value, @NotNull Consumer<T> setter,
            @NotNull Function<@Nullable T, @NotNull Component> displayNameMapper
    ) {
        final ButtonComponent button = Components.button(displayNameMapper.apply(value), b -> {});
        button.horizontalSizing(Sizing.expand());
        button.onPress(b -> {
            final Insets rootPadding = this.uiAdapter.rootComponent.padding().get();
            DropdownComponent.openContextMenu(
                    this, this.uiAdapter.rootComponent, FlowLayout::child,
                    button.getX() - rootPadding.left(), button.getY() + button.getHeight() - rootPadding.top(),
                    dropdown -> {
                        for (final T v : values) dropdown.button(displayNameMapper.apply(v), d -> {
                            setter.accept(v);
                            button.setMessage(displayNameMapper.apply(v));
                            dropdown.remove();
                        });
                    });
        });
        return row(key, required, button);
    }

    @NotNull
    private <T> FlowLayout checkBoxRow(
            @NotNull String key, boolean required,
            @NotNull Collection<T> availableValues, @NotNull Collection<T> selectedValues,
            @NotNull Function<T, @NotNull Component> displayNameMapper,
            @Nullable Function<T, @NotNull String> iconPathMapper
    ) {
        final FlowLayout checkboxes = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        checkboxes.gap(8);
        checkboxes.verticalAlignment(VerticalAlignment.CENTER);
        for (final T enumValue : availableValues) {
            final String iconPath = iconPathMapper == null ? null : iconPathMapper.apply(enumValue);
            checkboxes.child(iconPath == null
                    ? Components.checkbox(displayNameMapper.apply(enumValue))
                            .checked(selectedValues.contains(enumValue))
                            .onChanged(checked -> setChecked(checked, enumValue, selectedValues::add, selectedValues::remove))
                    : iconToggle(iconPath, displayNameMapper.apply(enumValue), selectedValues.contains(enumValue),
                            checked -> setChecked(checked, enumValue, selectedValues::add, selectedValues::remove)));
        }
        return row(key, required, checkboxes);
    }

    private static <T> void setChecked(boolean checked, @NotNull T value, @NotNull Consumer<T> adder, @NotNull Consumer<T> remover) {
        if (checked) {
            adder.accept(value);
        } else {
            remover.accept(value);
        }
    }

    private static final int ICON_TOGGLE_SIZE = 16;

    @NotNull
    private FlowLayout iconToggle(@NotNull String texturePath, @NotNull Component label, boolean initiallyChecked, @NotNull Consumer<Boolean> onChanged) {
        final FlowLayout box = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        box.gap(4);
        box.verticalAlignment(VerticalAlignment.CENTER);
        box.padding(Insets.of(3));
        box.child(Components.texture(VersionedIdentifier.of(texturePath), 0, 0, ICON_TOGGLE_SIZE, ICON_TOGGLE_SIZE, ICON_TOGGLE_SIZE, ICON_TOGGLE_SIZE));
        box.child(Components.label(label));

        final AtomicBoolean checked = new AtomicBoolean(initiallyChecked);
        final Runnable updateSurface = () -> box.surface(checked.get() ? Surface.outline(0xFF55FF55) : Surface.BLANK);
        updateSurface.run();
        //? if >=1.21.11 {
        /*box.mouseDown().subscribe((event, doubleClick) -> {
        *///?} else {
        box.mouseDown().subscribe((mouseX, mouseY, button) -> {
        //?}
            checked.set(!checked.get());
            onChanged.accept(checked.get());
            updateSurface.run();
            return true;
        });

        cursorAll(box, CursorStyle.HAND);
        return box;
    }

    @NotNull
    private FlowLayout row(@NotNull String key, boolean required, @NotNull io.wispforest.owo.ui.core.Component input) {
        final FlowLayout row = Containers.horizontalFlow(Sizing.fixed(500), Sizing.content());
        row.gap(8);
        row.verticalAlignment(VerticalAlignment.CENTER);

        final MutableComponent labelText = Component.empty();
        if (required) {
            labelText.append("*");
            labelText.withStyle(ChatFormatting.BOLD);
        }
        labelText.append(translatable("eventutils.event_builder.values." + key + ".label"));

        final LabelComponent label = Components.label(labelText);
        label.horizontalTextAlignment(HorizontalAlignment.RIGHT);
        label.horizontalSizing(Sizing.fixed(100));

        row.child(label);
        row.child(input);
        return row;
    }

    @NotNull
    private static FlowLayout inputWrapper(@NotNull io.wispforest.owo.ui.core.Component input, boolean invalid) {
        final FlowLayout wrapper = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        wrapper.padding(Insets.of(1));
        wrapper.child(input);
        markInvalid(wrapper, invalid);
        return wrapper;
    }

    private static void markInvalid(@NotNull FlowLayout wrapper, boolean invalid) {
        wrapper.surface(invalid ? Surface.outline(0xFFFF5555) : Surface.BLANK);
    }

    private static void cursorAll(@NotNull io.wispforest.owo.ui.core.Component component, @NotNull CursorStyle style) {
        component.cursorStyle(style);
        if (component instanceof ParentComponent parent) {
            for (final io.wispforest.owo.ui.core.Component child : parent.children()) cursorAll(child, style);
        }
    }

    @Override @NotNull
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    public static class Defaults {
        public static final boolean CUSTOM = false;
        @NotNull private static final Set<EAEvent.PingRole> PING_ROLES = Set.of(EAEvent.PingRole.PARTNER);
        @NotNull public static final String TITLE =  Minecraft.getInstance().getUser().getName() + "'s Event";
        @NotNull public static final String TIME = "15m";
        @NotNull private static final String IP = "play.eventalerts.gg";
        @NotNull public static final String DESCRIPTION = "";
        @NotNull private static final Set<EAEvent.Platform> PLATFORMS = Set.of(EAEvent.Platform.JAVA);
        @NotNull public static final String PRIZE = "";
        @Nullable public static final Integer MAX_PLAYERS = null;

        @NotNull
        public static Set<EAEvent.PingRole> pingRoles() {
            return new HashSet<>(PING_ROLES);
        }

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

        @NotNull
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
}
