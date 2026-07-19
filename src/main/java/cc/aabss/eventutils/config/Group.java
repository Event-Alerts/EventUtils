package cc.aabss.eventutils.config;

import cc.aabss.eventutils.versioning.VersionedGameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class Group {
    public static final int MAX_RADIUS = 100;

    @NotNull private String name = "New Group";
    @NotNull private Set<String> players = new HashSet<>();
    @NotNull private Set<String> entities = new HashSet<>();
    @NotNull private Mode playerMode = Mode.SHOW;
    @NotNull private Mode entityMode = Mode.HIDE;
    @NotNull private Mode nametagMode = Mode.SHOW;
    @NotNull private Mode npcMode = Mode.HIDE;
    /**
     * {@code null} = infinite
     */
    @Nullable @Range(from = 1, to = MAX_RADIUS) private Integer radius = null;

    public Group() {}

    public Group(@NotNull Group group) {
        this.name = group.name;
        this.players = new HashSet<>(group.players);
        this.entities = new HashSet<>(group.entities);
        this.playerMode = group.playerMode;
        this.entityMode = group.entityMode;
        this.nametagMode = group.nametagMode;
        this.npcMode = group.npcMode;
        this.radius = group.radius;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Group setName(@NotNull String name) {
        this.name = name;
        return this;
    }

    @NotNull
    public Set<String> getPlayers() {
        return players;
    }

    @NotNull
    public Group setPlayers(@NotNull Collection<String> players) {
        this.players = new HashSet<>(players);
        return this;
    }

    @NotNull
    public Set<String> getEntities() {
        return entities;
    }

    @NotNull
    public Group setEntities(@NotNull Collection<String> entities) {
        this.entities = new HashSet<>(entities);
        return this;
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
    public Group togglePlayerMode() {
        return setPlayerMode(playerMode == Mode.SHOW ? Mode.HIDE : Mode.SHOW);
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
    public Group toggleEntityMode() {
        return setEntityMode(entityMode == Mode.SHOW ? Mode.HIDE : Mode.SHOW);
    }

    @NotNull
    public Mode getNametagMode() {
        return nametagMode;
    }

    @NotNull
    public Group setNametagMode(@NotNull Mode nametagMode) {
        this.nametagMode = nametagMode;
        return this;
    }

    @NotNull
    public Group toggleNametagMode() {
        return setNametagMode(nametagMode == Mode.SHOW ? Mode.HIDE : Mode.SHOW);
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

    @NotNull
    public Group toggleNpcMode() {
        return setNpcMode(npcMode == Mode.SHOW ? Mode.HIDE : Mode.SHOW);
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

    public boolean outsideRadius(@Nullable Vec3d position) {
        if (radius == null || position == null) return false;
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return true;
        //? if >=1.21.11 {
        //final Vec3d clientPosition = client.player.getSyncedPos();
        //?} else {
        final Vec3d clientPosition = client.player.getPos();
         //?}
        return !(clientPosition.distanceTo(position) <= radius);
    }

    public boolean isPlayerVisible(@NotNull String name, @Nullable Vec3d position) {
        return (playerMode == Mode.SHOW) == players.contains(name.toLowerCase()) || outsideRadius(position);
    }

    public boolean isEntityVisible(@NotNull EntityType<?> entityType, @Nullable Vec3d position) {
        return (entityMode == Mode.SHOW) == entities.contains(entityType.getName().getString().toLowerCase()) || outsideRadius(position);
    }

    public boolean isNametagVisible(@NotNull VersionedGameProfile profile, @Nullable Vec3d position) {
        return nametagMode == Mode.SHOW || isPlayerVisible(profile.getName(), position);
    }

    public boolean isNametagVisible(@NotNull String name, @Nullable Vec3d position) {
        return nametagMode == Mode.SHOW || isPlayerVisible(name, position);
    }

    public boolean isNpcVisible(@Nullable Vec3d position) {
        return npcMode == Mode.SHOW || outsideRadius(position);
    }

    public enum Mode {
        SHOW,
        HIDE
    }
}
