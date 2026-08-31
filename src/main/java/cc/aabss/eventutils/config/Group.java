package cc.aabss.eventutils.config;

import cc.aabss.eventutils.stats.Statable;
import cc.aabss.eventutils.versioning.VersionedEntityType;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import xyz.srnyx.javautilities.parents.Stringable;
//? if >=26.2 {
//?}

import java.util.*;
import java.util.stream.Collectors;


public class Group extends Stringable implements Statable {
    public static final int MAX_RADIUS = 100;

    @NotNull private transient UUID uuid;
    @Nullable private String name;
    @NotNull private Set<String> players = Defaults.players();
    @NotNull private Set<EntityType<?>> entities = Defaults.entities();
    @NotNull private Mode playerMode = Defaults.PLAYER_MODE;
    @NotNull private Mode entityMode = Defaults.ENTITY_MODE;
    @NotNull private Mode npcMode = Defaults.NPC_MODE;
    /**
     * {@code null} = infinite
     */
    @Nullable @Range(from = 1, to = MAX_RADIUS) private Integer radius = Defaults.RADIUS;

    public Group() {
        this.uuid = UUID.randomUUID();
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public Group setUuid(@NotNull UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    @NotNull
    public String getName() {
        return Objects.requireNonNullElse(name, uuid.toString());
    }

    @NotNull
    public Group setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @NotNull
    public Set<String> getPlayers() {
        return players;
    }

    @NotNull
    public Group setPlayers(@NotNull Collection<String> players) {
        this.players = players.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return this;
    }

    @NotNull
    public Set<EntityType<?>> getEntities() {
        return entities;
    }

    @NotNull
    public Set<String> getEntityIds() {
        return entities.stream()
                .map(entityType -> EntityType.getKey(entityType).toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @NotNull
    public Group setEntities(@NotNull Collection<EntityType<?>> entities) {
        this.entities = new LinkedHashSet<>(entities);
        return this;
    }

    @NotNull
    public Group setEntitiesByIds(@NotNull Collection<String> entities) {
        return setEntities(entities.stream()
                .map(VersionedEntityType::getEntityTypeByIdentifier)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @NotNull
    public Mode getPlayerMode() {
        return playerMode;
    }

    @NotNull
    public Group setPlayerMode(@NotNull Mode playerMode) {
        this.playerMode = playerMode;
        return this;
    }

    @NotNull
    public Mode getEntityMode() {
        return entityMode;
    }

    @NotNull
    public Group setEntityMode(@NotNull Mode entityMode) {
        this.entityMode = entityMode;
        return this;
    }

    @NotNull
    public Mode getNpcMode() {
        return npcMode;
    }

    @NotNull
    public Group setNpcMode(@NotNull Mode npcMode) {
        this.npcMode = npcMode;
        return this;
    }

    @Nullable
    public Integer getRadius() {
        return radius;
    }

    @NotNull
    public Group setRadius(@Nullable Integer radius) {
        // Clamp
        if (radius != null) {
            if (radius <= 0) {
                radius = null;
            } else {
                radius = Math.min(radius, MAX_RADIUS);
            }
        }

        this.radius = radius;
        return this;
    }

    public boolean outsideRadius(@Nullable Vec3 position) {
        if (radius == null || position == null) return false;
        final Minecraft client = Minecraft.getInstance();
        if (client.player == null) return true;
        //? if >=1.21.11 {
        /*final Vec3 clientPosition = client.player.trackingPosition();
        *///?} else {
        final Vec3 clientPosition = client.player.position();
         //?}
        return clientPosition.distanceTo(position) > radius;
    }

    public boolean isPlayerVisible(@NotNull String name, @Nullable Vec3 position) {
        final boolean show = (playerMode == Mode.SHOW) == players.contains(name.toLowerCase());
        return show || outsideRadius(position);
    }

    public boolean isEntityVisible(@NotNull EntityType<?> entityType, @Nullable Vec3 position) {
        final boolean show = (entityMode == Mode.SHOW) == entities.contains(entityType);
        return show || outsideRadius(position);
    }

    public boolean isNpcVisible(@Nullable Vec3 position) {
        return npcMode == Mode.SHOW || outsideRadius(position);
    }

    public enum Mode {
        SHOW(ChatFormatting.GREEN),
        HIDE(ChatFormatting.RED);

        @NotNull public final ChatFormatting formatting;

        Mode(@NotNull ChatFormatting formatting) {
            this.formatting = formatting;
        }
    }

    @Override @Nullable
    public JsonObject toStat() {
        final JsonObject json = new JsonObject();
        json.addProperty("name", getName());
        json.addProperty("players_size", players.size());
        json.addProperty("entities_size", entities.size());
        json.addProperty("player_mode", playerMode.name());
        json.addProperty("entity_mode", entityMode.name());
        json.addProperty("npc_mode", npcMode.name());
        json.addProperty("radius", radius);
        return json;
    }

    // Collections/Maps need to have methods to create new instances of the collection!
    public static class Defaults {
        @NotNull private static final List<String> PLAYERS = List.of();
        @NotNull private static final List<EntityType<?>> ENTITIES = List.of();
        @NotNull public static final Mode PLAYER_MODE = Mode.SHOW;
        @NotNull public static final Mode ENTITY_MODE = Mode.HIDE;
        @NotNull public static final Mode NPC_MODE = Mode.HIDE;
        @Nullable public static final Integer RADIUS = null;

        @NotNull
        public static Set<String> players() {
            return new LinkedHashSet<>(PLAYERS);
        }
        @NotNull
        public static Set<EntityType<?>> entities() {
            return new LinkedHashSet<>(ENTITIES);
        }
        @NotNull
        public static Set<String> entityIds() {
            return ENTITIES.stream()
                    .map(entityType -> EntityType.getKey(entityType).toString())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
