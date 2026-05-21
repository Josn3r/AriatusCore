package net.ariatus.project.database.migration;

import net.ariatus.project.AriatusCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MigrationManager {

    private final AriatusCore core;
    private final List<DatabaseMigration> migrations = new ArrayList<>();
    private int lastExecutedCount;

    public MigrationManager(AriatusCore core) {
        this.core = core;
    }

    public int lastExecutedCount() {
        return lastExecutedCount;
    }

    public void register(DatabaseMigration migration) {
        migrations.add(migration);
        core.loggerService().debug("Migración registrada: " + migration.id());
    }

    public void runMigrations() {
        if (!core.databaseService().isConnected()) {
            core.loggerService().warn("No se ejecutaron migraciones: base de datos desconectada.");
            return;
        }

        core.databaseService().runAsync(connection -> {
            createMigrationsTable(connection);
            lastExecutedCount = 0;

            for (DatabaseMigration migration : migrations) {
                if (hasMigration(connection, migration.id())) {
                    continue;
                }

                core.loggerService().info("Ejecutando migración: " + migration.id());
                migration.migrate(connection);
                saveMigration(connection, migration.id());
                lastExecutedCount++;
            }
        });
    }

    private void createMigrationsTable(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS ariatus_migrations (
                    id VARCHAR(128) PRIMARY KEY,
                    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """)) {
            statement.executeUpdate();
        }
    }

    private boolean hasMigration(Connection connection, String id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM ariatus_migrations WHERE id = ?"
        )) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void saveMigration(Connection connection, String id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ariatus_migrations (id) VALUES (?)"
        )) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }
}