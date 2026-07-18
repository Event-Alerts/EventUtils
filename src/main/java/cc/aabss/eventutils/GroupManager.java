package cc.aabss.eventutils;

import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;


public class GroupManager {
    @NotNull private final EventUtils mod;

    @Nullable public UUID selectedGroup;
    @NotNull public Iterator<UUID> groupIterator;

    public GroupManager(@NotNull EventUtils mod) {
        this.mod = mod;
        resetIterator();
    }

    public boolean isAllPlayersRevealed() {
        return selectedGroup == null;
    }

    @Nullable
    public Group getSelectedGroup() {
        return selectedGroup != null ? mod.config.groups.get(selectedGroup) : null;
    }

    private void resetIterator() {
        groupIterator = mod.config.groups.keySet().iterator();
    }

    /**
     * Revealed -> group 1 -> group 2 -> revealed -> repeat
     */
    public void cycle() {
        if (selectedGroup == null) resetIterator();

        if (groupIterator.hasNext()) {
            selectedGroup = groupIterator.next();
        } else {
            selectedGroup = null;
        }
    }

    private boolean isSelf(@NotNull String name) {
        final MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.player != null && client.player.getName().getString().equalsIgnoreCase(name);
    }

    public boolean isPlayerVisible(@NotNull GameProfile profile, @Nullable Vec3d position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Always show self
        final VersionedGameProfile versionedProfile = new VersionedGameProfile(profile);
        if (isSelf(versionedProfile.getName())) return true;

        // Delegate to Group
        return group.isPlayerVisible(versionedProfile, position);
    }

    public boolean isPlayerVisible(@NotNull String name, @Nullable Vec3d position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Always show self
        if (isSelf(name)) return true;

        // Delegate to Group
        return group.isPlayerVisible(name, position);
    }

    public boolean isNametagVisible(@NotNull GameProfile profile, @Nullable Vec3d position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Always show self
        final VersionedGameProfile versionedProfile = new VersionedGameProfile(profile);
        if (isSelf(versionedProfile.getName())) return true;

        // Delegate to Group
        return group.isNametagVisible(versionedProfile, position);
    }

    public boolean isNametagVisible(@NotNull String name, @Nullable Vec3d position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Always show self
        if (isSelf(name)) return true;

        // Delegate to Group
        return group.isNametagVisible(name, position);
    }

    public boolean isEntityVisible(@NotNull EntityType<?> entityType, @Nullable Vec3d position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Delegate to Group
        return group.isEntityVisible(entityType, position);
    }
}
