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


public class GroupManager {
    @NotNull private final EventUtils mod;

    @Nullable public Group selectedGroup;

    public GroupManager(@NotNull EventUtils mod) {
        this.mod = mod;
    }

    /**
     * Revealed -> group 1 -> group 2 -> revealed -> repeat
     */
    public void cycle() {
        final int nextIndex = selectedGroup == null ? 0 : mod.config.groups.groups.indexOf(selectedGroup) + 1;
        selectedGroup = nextIndex < mod.config.groups.groups.size() ? mod.config.groups.groups.get(nextIndex) : null;
    }

    private boolean isSelf(@NotNull String name) {
        final Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.getName().getString().equalsIgnoreCase(name);
    }

    public boolean isPlayerVisible(@NotNull GameProfile profile, @Nullable Vec3 position) {
        // All visible
        if (selectedGroup == null) return true;

        // Always show self
        final VersionedGameProfile versionedProfile = new VersionedGameProfile(profile);
        if (isSelf(versionedProfile.getName())) return true;

        // NPC
        if (EventUtils.isNpc(versionedProfile.getId())) return selectedGroup.isNpcVisible(position);

        // Delegate to Group
        return selectedGroup.isPlayerVisible(versionedProfile.getName(), position);
    }

    public boolean isPlayerVisible(@Nullable String name, @Nullable Vec3 position) {
        // All visible
        if (selectedGroup == null) return true;

        // Always show self
        if (name != null && isSelf(name)) return true;

        // NPC
        if (EventUtils.isNpc(name)) return selectedGroup.isNpcVisible(position);

        // Delegate to Group
        return selectedGroup.isPlayerVisible(name, position);
    }

    public boolean isEntityVisible(@NotNull EntityType<?> entityType, @Nullable Vec3 position) {
        // All visible
        if (selectedGroup == null) return true;

        // Delegate to Group
        return selectedGroup.isEntityVisible(entityType, position);
    }
}
