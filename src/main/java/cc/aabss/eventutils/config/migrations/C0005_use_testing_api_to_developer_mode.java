package cc.aabss.eventutils.config.migrations;

import eu.okaeri.configs.migrate.builtin.NamedMigration;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;


public class C0005_use_testing_api_to_developer_mode extends NamedMigration {
    public C0005_use_testing_api_to_developer_mode() {
        super("migrates use_testing_api to developer_mode (2.3.0 or older)", move("use_testing_api", "developer_mode"));
    }
}
