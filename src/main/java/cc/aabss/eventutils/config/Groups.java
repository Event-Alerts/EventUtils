package cc.aabss.eventutils.config;

import cc.aabss.eventutils.stats.Statable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class Groups implements Statable {
    @NotNull public final List<Group> groups;

    public Groups(@NotNull Map<UUID, Group> groups) {
        this.groups = new ArrayList<>(groups.values());
    }

    public Groups(@NotNull Group @NotNull ... groups) {
        this.groups = new ArrayList<>(List.of(groups));
    }

    public Groups(@NotNull Groups groups) {
        this.groups = new ArrayList<>(groups.groups);
    }

    @NotNull
    public Map<UUID, Group> toMap() {
        return groups.stream().collect(Collectors.toMap(Group::getUuid, group -> group));
    }

    @Nullable
    public Group getByName(@NotNull String name) {
        return groups.stream()
                .filter(group -> group.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void remove(@NotNull UUID uuid) {
        groups.removeIf(group -> group.getUuid().equals(uuid));
    }

    @Override @NotNull
    public JsonElement toStat() {
        final JsonArray array = new JsonArray();
        for (final Group group : groups) array.add(group.toStat());
        return array;
    }
}
