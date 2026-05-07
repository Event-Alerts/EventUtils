package cc.aabss.eventutils.plustag;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.Versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Fetches player data from Event Alerts API to determine unlocked plus tags.
 * API: GET https://eventalerts.gg/api/v1/players/minecraft/uuid/{uuid}
 */
public final class EventAlertsApi {
    private static final String API_BASE = "https://eventalerts.gg";
    private static final String API_PATH = "/api/v1/players/minecraft/uuid/";

    private static final HttpClient HTTP = HttpClient.newBuilder().build();
    /** Cache: Minecraft UUID -> unlocked tags. Cleared on world unload. */
    private static final ConcurrentHashMap<UUID, EnumSet<PlusTag>> CACHE = new ConcurrentHashMap<>();
    /** UUIDs we've already scheduled a fetch for (avoid duplicate requests until cache clear). */
    private static final Set<UUID> FETCH_SCHEDULED = ConcurrentHashMap.newKeySet();

    private EventAlertsApi() {}

    @NotNull
    public static String getApiBase() {
        return EventUtils.MOD.config.useTestingApi ? "http://localhost:9090" : API_BASE;
    }

    @Nullable
    private static UUID parseMinecraftUuid(@NotNull String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Fetch unlocked plus tags for a Minecraft UUID. Returns empty set on failure. */
    @NotNull
    public static EnumSet<PlusTag> fetchUnlockedTags(@NotNull UUID minecraftUuid) {
        EnumSet<PlusTag> cached = CACHE.get(minecraftUuid);
        if (cached != null) {
            EventUtils.LOGGER.debug("[EventAlerts] fetchUnlockedTags: cache HIT uuid={} tags={}", minecraftUuid, cached);
            return cached;
        }

        EventUtils.LOGGER.info("[EventAlerts] Fetching tags for uuid={}", minecraftUuid);
        try {
            String url = getApiBase() + API_PATH + minecraftUuid;
            EventUtils.LOGGER.debug("[EventAlerts] GET {}", url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "EventUtils/" + Versions.EU_VERSION + " (Minecraft/" + Versions.MC_VERSION + ")")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            EventUtils.LOGGER.debug("[EventAlerts] response status={} bodyLength={}", response.statusCode(), response.body().length());
            if (response.statusCode() != 200) {
                EventUtils.LOGGER.warn("[EventAlerts] API returned status={} body={}", response.statusCode(), response.body());
                return EnumSet.noneOf(PlusTag.class);
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            // API may wrap data in "player": { ... } (player can be null if not found)
            JsonObject data;
            if (root.has("player") && !root.get("player").isJsonNull() && root.get("player").isJsonObject()) {
                data = root.getAsJsonObject("player");
                EventUtils.LOGGER.debug("[EventAlerts] using wrapped player object");
            } else if (!root.has("player")) {
                data = root;
                EventUtils.LOGGER.debug("[EventAlerts] using root as data (no player wrapper)");
            } else {
                EventUtils.LOGGER.info("[EventAlerts] API response: player=null (not linked or not found)");
                return EnumSet.noneOf(PlusTag.class); // player: null
            }
            EnumSet<PlusTag> unlocked = parseUnlockedTags(data);
            EventUtils.LOGGER.debug("[EventAlerts] parsed unlocked tags={} for uuid={}", unlocked, minecraftUuid);
            CACHE.put(minecraftUuid, unlocked);
            EventUtils.LOGGER.info("[EventAlerts] Fetched uuid={} tags={}", minecraftUuid, unlocked);
            return unlocked;
        } catch (Exception e) {
            EventUtils.LOGGER.warn("[EventAlerts] Fetch failed uuid={} error={}", minecraftUuid, e.getMessage(), e);
            return EnumSet.noneOf(PlusTag.class);
        }
    }

    /** Parse API response into unlocked tags. Expects player object: id, discord, minecraft, subscription (1=premium/Bee), anniversaries, roles (optional). */
    @NotNull
    private static EnumSet<PlusTag> parseUnlockedTags(@NotNull JsonObject root) {
        EnumSet<PlusTag> out = EnumSet.noneOf(PlusTag.class);

        // Linked: has discord and minecraft
        if (root.has("discord") && root.has("minecraft")) {
            JsonObject mc = root.getAsJsonObject("minecraft");
            if (mc != null && mc.has("uuid")) {
                out.add(PlusTag.NONE); // "Linked" icon
                EventUtils.LOGGER.debug("[EventAlerts] parse: +NONE (linked, discord+minecraft.uuid)");
            }
        }

        // Bee / Premium: subscription tier, premium flag, or subscription object
        if (root.has("subscription")) {
            JsonElement sub = root.get("subscription");
            if (sub != null && !sub.isJsonNull()) {
                if (sub.isJsonPrimitive()) {
                    if (sub.getAsJsonPrimitive().isNumber()) {
                        int v = sub.getAsInt();
                        if (v >= 1) {
                            out.add(PlusTag.ORANGE);
                            EventUtils.LOGGER.debug("[EventAlerts] parse: +ORANGE (subscription number={})", v);
                        }
                    } else if (sub.getAsJsonPrimitive().isBoolean() && sub.getAsBoolean()) {
                        out.add(PlusTag.ORANGE);
                        EventUtils.LOGGER.debug("[EventAlerts] parse: +ORANGE (subscription boolean true)");
                    }
                } else if (sub.isJsonObject()) {
                    JsonObject obj = sub.getAsJsonObject();
                    if (obj.has("tier") && obj.get("tier").getAsInt() >= 1) {
                        out.add(PlusTag.ORANGE);
                        EventUtils.LOGGER.debug("[EventAlerts] parse: +ORANGE (subscription.tier)");
                    } else if (obj.has("active") && obj.get("active").getAsBoolean()) {
                        out.add(PlusTag.ORANGE);
                        EventUtils.LOGGER.debug("[EventAlerts] parse: +ORANGE (subscription.active)");
                    }
                }
            }
        }
        if (root.has("premium") && root.get("premium").getAsBoolean()) {
            out.add(PlusTag.ORANGE);
            EventUtils.LOGGER.debug("[EventAlerts] parse: +ORANGE (premium)");
        }
        if (root.has("isPremium") && root.get("isPremium").getAsBoolean()) {
            out.add(PlusTag.ORANGE);
            EventUtils.LOGGER.debug("[EventAlerts] parse: +ORANGE (isPremium)");
        }

        // Booster: often a role or field. Check for "booster" boolean or roles array
        if (root.has("booster") && root.get("booster").getAsBoolean()) {
            out.add(PlusTag.PINK);
            EventUtils.LOGGER.debug("[EventAlerts] parse: +PINK (booster)");
        }
        if (root.has("roles")) {
            JsonArray roles = root.getAsJsonArray("roles");
            for (JsonElement e : roles) {
                String r = e.getAsString().toUpperCase();
                if (r.contains("BOOSTER")) { out.add(PlusTag.PINK); EventUtils.LOGGER.debug("[EventAlerts] parse: +PINK (roles contains BOOSTER)"); }
                if (r.contains("ADMIN")) { out.add(PlusTag.RED); EventUtils.LOGGER.debug("[EventAlerts] parse: +RED (roles contains ADMIN)"); }
                if (r.contains("DEV") || r.contains("CONTRIBUTOR")) { out.add(PlusTag.BLUE); EventUtils.LOGGER.debug("[EventAlerts] parse: +BLUE (roles DEV/CONTRIBUTOR)"); }
            }
        }

        // Some APIs use role names instead of IDs
        if (root.has("rolesNamed")) {
            JsonArray roles = root.getAsJsonArray("rolesNamed");
            for (JsonElement e : roles) {
                String r = e.getAsString().toUpperCase();
                if ("BOOSTER".equals(r)) { out.add(PlusTag.PINK); EventUtils.LOGGER.debug("[EventAlerts] parse: +PINK (rolesNamed BOOSTER)"); }
                if ("ADMIN".equals(r)) { out.add(PlusTag.RED); EventUtils.LOGGER.debug("[EventAlerts] parse: +RED (rolesNamed ADMIN)"); }
                if ("DEVELOPER".equals(r) || "CONTRIBUTOR".equals(r)) { out.add(PlusTag.BLUE); EventUtils.LOGGER.debug("[EventAlerts] parse: +BLUE (rolesNamed)"); }
            }
        }

        return out;
    }

    /** Clear cache (e.g. on disconnect). */
    public static void clearCache() {
        int size = CACHE.size();
        CACHE.clear();
        FETCH_SCHEDULED.clear();
        EventUtils.LOGGER.info("[EventAlerts] Cache cleared (was {} entries)", size);
    }

    /** Schedule a fetch for this UUID if not cached and not already scheduled. Call from main thread. */
    public static void scheduleFetchIfNeeded(@NotNull UUID minecraftUuid) {
        if (CACHE.containsKey(minecraftUuid)) return;
        if (!FETCH_SCHEDULED.add(minecraftUuid)) return; // already scheduled
        EventUtils.LOGGER.info("[EventAlerts] Scheduling fetch for uuid={}", minecraftUuid);
        EventUtils.MOD.scheduler.execute(() -> EventAlertsApi.fetchUnlockedTags(minecraftUuid));
    }

    public static void scheduleFetchIfNeeded(@NotNull String minecraftUuid) {
        UUID uuid = parseMinecraftUuid(minecraftUuid);
        if (uuid != null) scheduleFetchIfNeeded(uuid);
    }

    /** Get cached unlocked tags for UUID, or null if not cached. (No per-call debug log to avoid spam from tab list.) */
    @Nullable
    public static EnumSet<PlusTag> getCached(@NotNull UUID minecraftUuid) {
        return CACHE.get(minecraftUuid);
    }

    @Nullable
    public static EnumSet<PlusTag> getCached(@NotNull String minecraftUuid) {
        UUID uuid = parseMinecraftUuid(minecraftUuid);
        return uuid != null ? CACHE.get(uuid) : null;
    }
}
