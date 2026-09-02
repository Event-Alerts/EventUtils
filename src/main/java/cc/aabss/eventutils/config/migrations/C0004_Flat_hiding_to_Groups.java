package cc.aabss.eventutils.config.migrations;

import cc.aabss.eventutils.config.Group;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import net.minecraft.world.entity.EntityType;

import java.util.List;
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

                            // Create Legacy Group
                            final UUID uuid = UUID.randomUUID();
                            view.set("groups." + uuid + ".name", "Legacy Group", String.class);
                            view.set("groups." + uuid + ".player_mode", Group.Mode.SHOW, Group.Mode.class);
                            view.set("groups." + uuid + ".entity_mode", Group.Mode.HIDE, Group.Mode.class);
                            if (whitelistedPlayers != null) view.set("groups." + uuid + ".players", whitelistedPlayers, GenericsDeclaration.of(List.class, List.of(String.class)));
                            if (hiddenEntityTypes != null) view.set("groups." + uuid + ".entities", hiddenEntityTypes, GenericsDeclaration.of(List.class, List.of(EntityType.class)));
                            if (hidePlayersRadius != null) view.set("groups." + uuid + ".radius", hidePlayersRadius, Integer.class);
                            if (hideNpcs != null) view.set("groups." +uuid + ".npc_mode", hideNpcs ? Group.Mode.HIDE : Group.Mode.SHOW, Group.Mode.class);

                            // Remove legacy settings
                            view.remove("whitelisted_players");
                            view.remove("hidden_entity_types");
                            view.remove("hide_players_radius");
                            view.remove("hide_npcs");
                            return true;
                        })));
    }
}
