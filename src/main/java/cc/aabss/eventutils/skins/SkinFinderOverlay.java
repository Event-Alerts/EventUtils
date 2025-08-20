package cc.aabss.eventutils.skins;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.EventConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fullscreen HUD overlay to search EventSkins and select a skin username to copy.
 * Uses Fabric HUD render callback and direct mouse/key polling; avoids creating a Screen.
 */
public final class SkinFinderOverlay {
    private static final AtomicBoolean OPEN = new AtomicBoolean(false);
    @Nullable private static String currentPrompt;
    private static final List<SkinItem> results = new CopyOnWriteArrayList<>();
    private static volatile boolean loading = false;
    @Nullable private static String errorMessage;

    // Layout state
    private static int lastMouseX;
    private static int lastMouseY;
    private static boolean mouseDown;
    private static boolean clickEdge;
    private static boolean prevCursorLocked;

    // Scrolling state
    private static int scrollOffsetPx;
    private static boolean draggingScrollbar;
    private static int dragScrollbarDeltaY;
    private static int currentViewportH;
    private static int currentContentH;
    private static int currentGridTop;
    private static int currentTrackX;
    private static int currentTrackW;
    private static int currentTrackH;
    private static int currentThumbY;
    private static int currentThumbH;

    // GLFW hooks
    private static boolean glfwScrollHookInstalled;
    private static GLFWScrollCallbackI previousScrollCallback;
    private static boolean glfwMouseHookInstalled;
    private static GLFWMouseButtonCallbackI previousMouseButtonCallback;
    private static boolean glfwKeyHookInstalled;
    private static GLFWKeyCallbackI previousKeyCallback;

    // Image cache state
    private static final java.util.concurrent.ConcurrentHashMap<String, Identifier> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, int[]> IMAGE_SIZE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> IMAGE_LOADING = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isOpen() { return OPEN.get(); }

    private static final HudRenderCallback RENDERER = (drawContext, tickDelta) -> {
        if (!OPEN.get()) return;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;

        final int sw = client.getWindow().getScaledWidth();
        final int sh = client.getWindow().getScaledHeight();
        final int pad = 8;
        final int maxW = Math.min(320, (int) Math.round(sw * 0.80));
        final int panelW = Math.min(sw - pad * 2, Math.max(maxW, (int) (sw * 0.55)));
        final int panelH = Math.min(sh - pad * 2, (int) (sh * 0.8));
        final int panelX = (sw - panelW) / 2;
        final int panelY = (sh - panelH) / 2;

        // Backdrop
        drawContext.fill(0, 0, sw, sh, 0x99000000);
        // Panel
        drawContext.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC0E1116);
        drawContext.fill(panelX, panelY, panelX + panelW, panelY + 36, 0xFF12161F);

        final TextRenderer tr = client.textRenderer;
        final String leftTitle = "Skin Picker: ";
        final String rightTitle = currentPrompt == null ? "" : currentPrompt;
        final int titleX = panelX + 12;
        final int titleY = panelY + 12;
        drawContext.drawText(tr, leftTitle, titleX, titleY, 0x7CC7FF, false);
        drawContext.drawText(tr, rightTitle, titleX + tr.getWidth(leftTitle), titleY, 0xB9B9C3, false);

        if (loading) {
            final String msg = "Searching…";
            drawContext.drawText(tr, msg, panelX + 12, panelY + 48, 0xB9B9C3, false);
            return;
        }

        if (errorMessage != null) {
            final int errBg = 0xFF3B0D0D;
            final int errX = panelX + 12;
            final int errY = panelY + 44;
            final int errW = Math.min(panelW - 24, tr.getWidth(errorMessage) + 16);
            final int errH = 20;
            drawContext.fill(errX, errY, errX + errW, errY + errH, errBg);
            drawContext.drawText(tr, errorMessage, errX + 8, errY + 6, 0xFFD7D7, false);
            return;
        }

        // Grid geometry
        final int gridTop = panelY + 44;
        final int gridLeft = panelX + 12;
        final int gridRight = panelX + panelW - 12;
        final int cardW = 58;
        final int cardH = 116;
        final int gap = 8;
        final int viewportH = panelY + panelH - 12 - gridTop;
        final int trackW = 6;
        final int trackX = panelX + panelW - 8; // overlay on right padding
        final int trackH = viewportH;

        // Compute content height
        final int columnWidth = cardW + gap;
        final int availableWidth = gridRight - gridLeft + gap;
        final int columns = Math.max(1, availableWidth / columnWidth);
        final int rows = (int) Math.ceil(results.size() / (double) columns);
        final int contentH = Math.max(0, rows * (cardH + gap) - gap);

        currentViewportH = viewportH;
        currentContentH = contentH;
        currentGridTop = gridTop;
        currentTrackX = trackX;
        currentTrackW = trackW;
        currentTrackH = trackH;

        // Clamp scroll offset
        final int maxScroll = Math.max(0, contentH - viewportH);
        if (scrollOffsetPx < 0) scrollOffsetPx = 0;
        if (scrollOffsetPx > maxScroll) scrollOffsetPx = maxScroll;

        // Scissor to viewport
        drawContext.enableScissor(gridLeft, gridTop, gridRight, gridTop + viewportH);

        // Draw cards with offset
        int x = gridLeft;
        int y = gridTop - scrollOffsetPx;
        for (final SkinItem item : results) {
            if (x + cardW > gridRight) {
                x = gridLeft;
                y += cardH + gap;
            }
            // Only draw if intersecting viewport
            if (y + cardH >= gridTop && y <= gridTop + viewportH) drawCard(drawContext, tr, item, x, y, cardW, cardH);
            x += cardW + gap;
        }

        drawContext.disableScissor();

        // Scrollbar
        if (contentH > viewportH) {
            final double visibleRatio = viewportH / (double) contentH;
            int thumbH = Math.max(18, (int) Math.round(visibleRatio * trackH));
            if (thumbH > trackH) thumbH = trackH;
            final int trackRange = trackH - thumbH;
            final int thumbY = gridTop + (maxScroll == 0 ? 0 : (int) Math.round((scrollOffsetPx / (double) maxScroll) * trackRange));
            currentThumbY = thumbY;
            currentThumbH = thumbH;

            final int trackCol = 0x33FFFFFF;
            final int thumbCol = within(lastMouseX, lastMouseY, trackX, gridTop, trackW, trackH) ? 0xAAFFFFFF : 0x88FFFFFF;
            drawContext.fill(trackX, gridTop, trackX + trackW, gridTop + trackH, trackCol);
            drawContext.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, thumbCol);
        } else {
            currentThumbY = 0;
            currentThumbH = 0;
        }

        // Click handling (edge-triggered)
        if (clickEdge) {
            clickEdge = false;
            // Scrollbar click/drag begin
            if (contentH > viewportH && within(lastMouseX, lastMouseY, trackX, gridTop, trackW, trackH)) {
                if (within(lastMouseX, lastMouseY, trackX, currentThumbY, trackW, currentThumbH)) {
                    draggingScrollbar = true;
                    dragScrollbarDeltaY = lastMouseY - currentThumbY;
                } else {
                    // Jump to position
                    final int newThumbY = Math.max(gridTop, Math.min(gridTop + trackH - currentThumbH, lastMouseY - currentThumbH / 2));
                    final int trackRange = trackH - currentThumbH;
                    final int maxScrollNow = Math.max(0, contentH - viewportH);
                    final double ratio = (newThumbY - gridTop) / (double) Math.max(1, trackRange);
                    scrollOffsetPx = (int) Math.round(ratio * maxScrollNow);
                }
                return; // don't treat as card click
            }

            // Card click handling
            x = gridLeft;
            y = gridTop - scrollOffsetPx;
            for (final SkinItem item : results) {
                if (x + cardW > gridRight) {
                    x = gridLeft;
                    y += cardH + gap;
                }
                if (within(lastMouseX, lastMouseY, x, y, cardW, cardH)) {
                    final MinecraftClient client2 = MinecraftClient.getInstance();
                    if (item.id != null && client2 != null) {
                        client2.inGameHud.setOverlayMessage(Text.literal("Finding a player wearing this skin…"), false);
                        handleAsync(item.id);
                    }
                    break;
                }
                x += cardW + gap;
            }
        }
    };

    private static void drawCard(@NotNull DrawContext ctx, @NotNull TextRenderer tr, @NotNull SkinItem item, int x, int y, int w, int h) {
        // Card background
        ctx.fill(x, y, x + w, y + h, 0xFF11141B);
        // Position pill
        final String pill = "#" + item.position;
        final int pillW = tr.getWidth(pill) + 10;
        ctx.fill(x + 6, y + 6, x + 6 + pillW, y + 20, 0xFF1E2633);
        ctx.drawText(tr, pill, x + 11, y + 10, 0xCDD3E0, false);

        // Meta bottom bar
        final String age = item.ageLabel == null ? "" : item.ageLabel;
        final String weight = Math.round(item.weight * 100) + "%";
        final String meta = age.isEmpty() ? weight : (age + "  " + weight);
        ctx.drawText(tr, meta, x + 6, y + h - 12, 0xB9B9C3, false);

        // Hero image area
        final int imgX = x + 1;
        final int imgY = y + 22;
        final int imgW = w - 2;
        final int imgH = h - 44;
        ctx.fill(imgX, imgY, imgX + imgW, imgY + imgH, 0xFF0D0F14);
        if (item.imageUrl != null && !item.imageUrl.isBlank()) {
            final Identifier id = IMAGE_CACHE.get(item.imageUrl);
            final int[] size = IMAGE_SIZE.get(item.imageUrl);
            if (id != null && size != null && size.length == 2) {
                // Preserve aspect ratio, shrink slightly, center in area, and sample full image
                final int texW = Math.max(1, size[0]);
                final int texH = Math.max(1, size[1]);
                final double scale = 0.94 * Math.min(imgW / (double) texW, imgH / (double) texH);
                final int drawW = Math.max(1, (int) Math.round(texW * scale));
                final int drawH = Math.max(1, (int) Math.round(texH * scale));
                final int drawX = imgX + (imgW - drawW) / 2;
                final int drawY = imgY + (imgH - drawH) / 2;
                ctx.drawTexture(RenderLayer::getGuiTextured, id, drawX, drawY, 0.0f, 0.0f, drawW, drawH, texW, texH, texW, texH);
            } else if (IMAGE_LOADING.putIfAbsent(item.imageUrl, Boolean.TRUE) == null) {
                downloadImage(item.imageUrl);
            }
        }

        // Hover select overlay
        if (within(lastMouseX, lastMouseY, x, y, w, h)) {
            ctx.fill(x, y, x + w, y + h, 0x59000000);
            final String btn = "Use";
            final int bw = tr.getWidth(btn) + 16;
            final int bx = x + (w - bw) / 2;
            final int by = y + (h - 18) / 2;
            ctx.fill(bx, by, bx + bw, by + 18, 0xFF10B981);
            ctx.drawText(tr, btn, bx + 8, by + 5, 0xFF00140D, false);
        }
    }

    private static boolean within(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static void copyToClipboard(@NotNull String text) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.keyboard.setClipboard(text);
    }

    public static void open(@NotNull String prompt) {
        currentPrompt = prompt;
        errorMessage = null;
        results.clear();
        scrollOffsetPx = 0;
        OPEN.set(true);
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.mouse != null) {
            prevCursorLocked = client.mouse.isCursorLocked();
            client.mouse.unlockCursor();
        }
        ensureCallbacksRegistered();
        searchAsync(prompt);
    }

    public static void close() {
        OPEN.set(false);
        currentPrompt = null;
        errorMessage = null;
        results.clear();
        draggingScrollbar = false;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.mouse != null && prevCursorLocked) client.mouse.lockCursor();
    }

    private static void ensureCallbacksRegistered() {
        // Register only once
        if (!callbacksRegistered) {
            HudRenderCallback.EVENT.register(RENDERER);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client == null || client.getWindow() == null) return;
                if (!OPEN.get()) return;
                lastMouseX = (int) (client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth());
                lastMouseY = (int) (client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight());
                final long window = client.getWindow().getHandle();
                // Keep cursor unlocked while open
                if (client.mouse.isCursorLocked()) client.mouse.unlockCursor();

                // Dragging scrollbar
                if (draggingScrollbar && currentContentH > currentViewportH) {
                    final int trackRange = Math.max(1, currentTrackH - currentThumbH);
                    int newThumbY = lastMouseY - dragScrollbarDeltaY;
                    if (newThumbY < currentGridTop) newThumbY = currentGridTop;
                    if (newThumbY > currentGridTop + trackRange) newThumbY = currentGridTop + trackRange;
                    final double ratio = (newThumbY - currentGridTop) / (double) trackRange;
                    final int maxScroll = Math.max(0, currentContentH - currentViewportH);
                    scrollOffsetPx = (int) Math.round(ratio * maxScroll);
                }
            });
            callbacksRegistered = true;
        }
        // Install GLFW scroll hook once
        if (!glfwScrollHookInstalled) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                final long window = client.getWindow().getHandle();
                previousScrollCallback = GLFW.glfwSetScrollCallback(window, (win, xoff, yoff) -> {
                    if (OPEN.get() && currentContentH > currentViewportH) {
                        final int step = 48;
                        scrollOffsetPx -= (int) Math.round(yoff * step);
                        final int maxScroll = Math.max(0, currentContentH - currentViewportH);
                        if (scrollOffsetPx < 0) scrollOffsetPx = 0;
                        if (scrollOffsetPx > maxScroll) scrollOffsetPx = maxScroll;
                        return; // consume
                    }
                    if (previousScrollCallback != null) previousScrollCallback.invoke(win, xoff, yoff);
                });
                glfwScrollHookInstalled = true;
            }
        }
        // Install GLFW mouse button hook once
        if (!glfwMouseHookInstalled) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                final long window = client.getWindow().getHandle();
                previousMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
                    if (OPEN.get()) {
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            if (action == GLFW.GLFW_PRESS) { clickEdge = true; mouseDown = true; }
                            if (action == GLFW.GLFW_RELEASE) { mouseDown = false; draggingScrollbar = false; }
                        }
                        return; // consume
                    }
                    if (previousMouseButtonCallback != null) previousMouseButtonCallback.invoke(win, button, action, mods);
                });
                glfwMouseHookInstalled = true;
            }
        }
        // Install GLFW key hook once
        if (!glfwKeyHookInstalled) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                final long window = client.getWindow().getHandle();
                previousKeyCallback = GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
                    if (OPEN.get()) {
                        if (action == GLFW.GLFW_PRESS) {
                            // ESC closes overlay
                            if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return; }
                            // Consume typical GUI/chat keys
                            if (key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_T || key == GLFW.GLFW_KEY_SLASH || key == GLFW.GLFW_KEY_L || key == GLFW.GLFW_KEY_TAB) {
                                return;
                            }
                        }
                        if (action == GLFW.GLFW_REPEAT) {
                            if (key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_T || key == GLFW.GLFW_KEY_SLASH || key == GLFW.GLFW_KEY_L || key == GLFW.GLFW_KEY_TAB) {
                                return;
                            }
                        }
                    }
                    if (previousKeyCallback != null) previousKeyCallback.invoke(win, key, scancode, action, mods);
                });
                glfwKeyHookInstalled = true;
            }
        }
    }

    private static volatile boolean callbacksRegistered = false;

    private static void searchAsync(@NotNull String prompt) {
        loading = true;
        final EventConfig config = EventUtils.MOD.config;
        final String base = config.getEventSkinsAPIHost();
        final String url = base + "/api/list/" + uriEncode(prompt);

        httpGet(url).thenAccept(body -> {
            try {
                final JsonObject root = JsonParserShim.parse(body).getAsJsonObject();
                final JsonArray items = root.has("items") && root.get("items").isJsonArray() ? root.get("items").getAsJsonArray() : new JsonArray();
                final List<SkinItem> parsed = new ArrayList<>();
                for (final JsonElement el : items) {
                    if (!el.isJsonObject()) continue;
                    final JsonObject it = el.getAsJsonObject();
                    final String id = getString(it, "id");
                    final int position = getInt(it, "position", parsed.size() + 1);
                    final double weight = getDouble(it, "weight", 0);
                    final String imageUrl = getString(it, "imageUrl");
                    String ageLabel = null;
                    if (it.has("age") && it.get("age").isJsonObject()) {
                        final JsonObject age = it.getAsJsonObject("age");
                        final Integer val = age.has("value") && age.get("value").isJsonPrimitive() ? age.get("value").getAsInt() : null;
                        final String unit = age.has("unit") && age.get("unit").isJsonPrimitive() ? age.get("unit").getAsString() : null;
                        if (val != null && unit != null) ageLabel = val + unit;
                    } else if (it.has("ageYearsApprox") && it.get("ageYearsApprox").isJsonPrimitive()) {
                        ageLabel = String.format("%.2fy", it.get("ageYearsApprox").getAsDouble());
                    }
                    parsed.add(new SkinItem(id, position, weight, ageLabel, imageUrl));
                }
                results.clear();
                results.addAll(parsed);
                errorMessage = parsed.isEmpty() ? "No results for this tag." : null;
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to parse EventSkins list response", e);
                errorMessage = "Failed to load results";
            }
        }).exceptionally(ex -> {
            EventUtils.LOGGER.error("EventSkins list request failed", ex);
            errorMessage = "Failed to load results";
            return null;
        }).whenComplete((v, t) -> loading = false);
    }

    private static void handleAsync(@NotNull String skinId) {
        final EventConfig config = EventUtils.MOD.config;
        final String base = config.getEventSkinsAPIHost();
        final String url = base + "/api/handle/" + uriEncode(skinId);
        System.out.println("EventSkins handle request: " + url);
        httpGet(url).thenAccept(body -> {
            try {
                final JsonObject root = JsonParserShim.parse(body).getAsJsonObject();
                final boolean matched = root.has("matched") && root.get("matched").getAsBoolean();
                final String username = root.has("username") && root.get("username").isJsonPrimitive() ? root.get("username").getAsString() : null;
                final MinecraftClient client = MinecraftClient.getInstance();
                if (!matched || username == null || username.isEmpty()) {
                    errorMessage = "No player is currently wearing this skin.";
                    if (client != null) client.inGameHud.setOverlayMessage(Text.literal(errorMessage), false);
                    return;
                }
                copyToClipboard(username);
                if (client != null) client.inGameHud.setOverlayMessage(Text.literal("Copied username: " + username), false);
                close();
            } catch (final Exception e) {
                EventUtils.LOGGER.error("Failed to parse EventSkins handle response", e);
                errorMessage = "Failed to find a player for this skin";
            }
        }).exceptionally(ex -> {
            EventUtils.LOGGER.error("EventSkins handle request failed", ex);
            errorMessage = "No player is currently wearing this skin.";
            return null;
        });
    }

    private static CompletableFuture<String> httpGet(@NotNull String url) {
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 404) throw new RuntimeException("404");
                        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new RuntimeException("HTTP " + response.statusCode());
                        return Objects.requireNonNullElse(response.body(), "{}");
                    })
                    .whenComplete((body, t) -> {
                        //? if java: >=21
                        client.close();
                    });
        } catch (final Exception e) {
            final CompletableFuture<String> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    private static String uriEncode(@NotNull String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static int getInt(@NotNull JsonObject obj, @NotNull String key, int def) {
        try { return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : def; } catch (Exception ignored) { return def; }
    }
    private static double getDouble(@NotNull JsonObject obj, @NotNull String key, double def) {
        try { return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsDouble() : def; } catch (Exception ignored) { return def; }
    }
    @Nullable private static String getString(@NotNull JsonObject obj, @NotNull String key) {
        try { return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null; } catch (Exception ignored) { return null; }
    }

    private record SkinItem(@Nullable String id, int position, double weight, @Nullable String ageLabel, @Nullable String imageUrl) {}

    // Minimal JSON parser shim to avoid adding gson streams here
    private static final class JsonParserShim {
        static com.google.gson.JsonElement parse(String body) {
            return com.google.gson.JsonParser.parseString(body);
        }
    }

    private static void downloadImage(@NotNull String url) {
        try {
            final HttpClient http = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
            http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(response -> {
                try {
                    if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) throw new RuntimeException("HTTP " + response.statusCode());
                    final byte[] data = response.body();
                    final MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc == null) return;
                    mc.execute(() -> {
                        try {
                            final NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(data));
                            final int texW = image.getWidth();
                            final int texH = image.getHeight();
                            final NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                            final Identifier id = Identifier.of("eventutils", "skin_finder/" + Integer.toHexString(url.hashCode()));
                            mc.getTextureManager().registerTexture(id, texture);
                            IMAGE_CACHE.put(url, id);
                            IMAGE_SIZE.put(url, new int[]{texW, texH});
                        } catch (final Exception e) {
                            EventUtils.LOGGER.warn("Failed to decode image: {}", url, e);
                        } finally {
                            IMAGE_LOADING.remove(url);
                        }
                    });
                } catch (final Exception e) {
                    EventUtils.LOGGER.warn("Failed downloading image: {}", url, e);
                    IMAGE_LOADING.remove(url);
                } finally {
                    //? if java: >=21
                    http.close();
                }
            }).exceptionally(ex -> {
                EventUtils.LOGGER.warn("Image request failed: {}", url, ex);
                //? if java: >=21
                try { http.close(); } catch (Exception ignored) {}
                IMAGE_LOADING.remove(url);
                return null;
            });
        } catch (final Exception e) {
            EventUtils.LOGGER.warn("Failed to queue image download: {}", url, e);
            IMAGE_LOADING.remove(url);
        }
    }
}


