package cc.aabss.eventutils.config.migrations;

import cc.aabss.eventutils.config.Group;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import net.minecraft.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;


public class C0004_Flat_hiding_to_Groups extends NamedMigration {
    public C0004_Flat_hiding_to_Groups() {
        super("migrates flat hiding settings to Groups (2.3.0 or older)",
                when(
                        any(
                                exists("whitelisted_players"),
                                exists("hidden_entity_types"),
                                exists("hide_players_radius"),
                                exists("hide_npcs")),

                        ((config, view) -> {
                            // Get legacy settings
                            final List<String> whitelistedPlayers = view.get("whitelisted_players", GenericsDeclaration.of(List.class, List.of(String.class)));
                            final List<EntityType<?>> hiddenEntityTypes = view.get("hidden_entity_types", GenericsDeclaration.of(List.class, List.of(EntityType.class)));
                            final Integer hidePlayersRadius = view.get("hide_players_radius", Integer.class);
                            final Boolean hideNpcs = view.get("hide_npcs", Boolean.class);

                            // Create Group
                            final Group group = new Group()
                                    .setName("Legacy Group")
                                    .setPlayerMode(Group.Mode.SHOW)
                                    .setEntityMode(Group.Mode.HIDE);
                            if (whitelistedPlayers != null) group.setPlayers(whitelistedPlayers);
                            if (hiddenEntityTypes != null) group.setEntities(hiddenEntityTypes);
                            if (hidePlayersRadius != null) group.setRadius(hidePlayersRadius);
                            if (hideNpcs != null) group.setNpcMode(hideNpcs ? Group.Mode.HIDE : Group.Mode.SHOW);

                            // Update groups
                            Map<UUID, Group> groups = view.get("groups", GenericsDeclaration.of(Map.class, List.of(UUID.class, Group.class)));
                            if (groups == null) groups = new HashMap<>();
                            groups.put(group.getUuid(), group);
                            view.set("groups", groups);

                            // Remove legacy settings
                            view.remove("whitelisted_players");
                            view.remove("hidden_entity_types");
                            view.remove("hide_players_radius");
                            view.remove("hide_npcs");
                            return true;
                        })));
    }
}
