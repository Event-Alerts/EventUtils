package cc.aabss.eventutils.config.migrations;

import eu.okaeri.configs.migrate.builtin.NamedMigration;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;


public class C0001_Rename_kebab_case_to_snake_case extends NamedMigration {
    public C0001_Rename_kebab_case_to_snake_case() {
        super("renames kebab-case keys to snake_case (older than 2.0.0)",
                move("discord-rpc", "discord_rpc"),
                move("auto-tp", "auto_tp"),
                move("simple-queue-msg", "simple_queue_msg"),
                move("update-checker", "update_checker"),
                move("confirm-window-close", "confirm_window_close"),
                move("confirm-disconnect", "confirm_disconnect"),
                move("default-famous-ip", "default_famous_ip"),
                move("whitelisted-players", "whitelisted_players"));
    }
}
