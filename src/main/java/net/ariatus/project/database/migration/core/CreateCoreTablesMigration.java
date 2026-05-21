package net.ariatus.project.database.migration.core;

import net.ariatus.project.database.migration.DatabaseMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CreateCoreTablesMigration implements DatabaseMigration {

    @Override
    public String id() {
        return "core_001_create_core_tables";
    }

    @Override
    public void migrate(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS ariatus_core_status (
                    id INT PRIMARY KEY,
                    server_name VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """)) {
            statement.executeUpdate();
        }
    }
}