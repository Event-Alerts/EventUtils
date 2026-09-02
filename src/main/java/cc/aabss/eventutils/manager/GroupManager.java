package cc.aabss.eventutils.manager;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.config.Group;
import cc.aabss.eventutils.versioning.VersionedGameProfile;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
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
        final Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.getName().getString().equalsIgnoreCase(name);
    }

    public boolean isPlayerVisible(@NotNull GameProfile profile, @Nullable Vec3 position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Always show self
        final VersionedGameProfile versionedProfile = new VersionedGameProfile(profile);
        if (isSelf(versionedProfile.getName())) return true;

        // NPC
        if (EventUtils.isNpc(versionedProfile.getId())) return group.isNpcVisible(position);

        // Delegate to Group
        return group.isPlayerVisible(versionedProfile.getName(), position);
    }

    public boolean isPlayerVisible(@Nullable String name, @Nullable Vec3 position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Always show self
        if (name != null && isSelf(name)) return true;

        // NPC
        if (EventUtils.isNpc(name)) return group.isNpcVisible(position);

        // Delegate to Group
        return group.isPlayerVisible(name, position);
    }

    public boolean isEntityVisible(@NotNull EntityType<?> entityType, @Nullable Vec3 position) {
        // All visible
        final Group group = getSelectedGroup();
        if (group == null) return true;

        // Delegate to Group
        return group.isEntityVisible(entityType, position);
    }
}
