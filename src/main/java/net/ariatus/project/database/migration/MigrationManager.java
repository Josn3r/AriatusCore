package net.ariatus.project.database.migration;

import net.ariatus.project.AriatusCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public final class MigrationManager {

    private static final String CORE_OWNER = "core";

    private final AriatusCore core;

    private final List<DatabaseMigration> coreMigrations =
            new CopyOnWriteArrayList<>();

    private final Map<String, ReentrantLock> ownerLocks =
            new ConcurrentHashMap<>();

    private final Map<String, Integer> lastExecuted =
            new ConcurrentHashMap<>();

    public MigrationManager(AriatusCore core) {
        this.core = Objects.requireNonNull(
                core,
                "core"
        );
    }

    public void register(DatabaseMigration migration) {
        Objects.requireNonNull(
                migration,
                "migration"
        );

        validateMigrationId(
                migration.id()
        );

        boolean duplicate =
                coreMigrations.stream()
                        .anyMatch(existing ->
                                existing.id().equalsIgnoreCase(
                                        migration.id()
                                )
                        );

        if (duplicate) {
            throw new IllegalStateException(
                    "Migración Core duplicada: "
                            + migration.id()
            );
        }

        coreMigrations.add(migration);

        core.loggerService().debug(
                "Migración Core registrada: "
                        + migration.id()
        );
    }

    public int runMigrations() {
        if (!core.databaseService().isConnected()) {
            core.loggerService().warn(
                    "No se ejecutaron migraciones Core: base de datos no disponible."
            );

            lastExecuted.put(
                    CORE_OWNER,
                    0
            );

            return 0;
        }

        int count = run(
                CORE_OWNER,
                coreMigrations
        );

        lastExecuted.put(
                CORE_OWNER,
                count
        );

        return count;
    }

    public int run(
            String ownerId,
            DatabaseMigration... migrations
    ) {
        Objects.requireNonNull(
                migrations,
                "migrations"
        );

        return run(
                ownerId,
                List.of(migrations)
        );
    }

    public int run(
            String ownerId,
            Collection<? extends DatabaseMigration> migrations
    ) {
        String owner =
                normalizeOwner(ownerId);

        Objects.requireNonNull(
                migrations,
                "migrations"
        );

        core.databaseService()
                .requireConnected();

        validateMigrations(
                migrations
        );

        ReentrantLock lock =
                ownerLocks.computeIfAbsent(
                        owner,
                        ignored -> new ReentrantLock()
                );

        lock.lock();

        try {
            int executed =
                    core.databaseService()
                            .executeBlocking(connection -> {
                                createMigrationTable(
                                        connection
                                );

                                int count = 0;

                                for (
                                        DatabaseMigration migration :
                                        migrations
                                ) {
                                    if (
                                            hasMigration(
                                                    connection,
                                                    owner,
                                                    migration.id()
                                            )
                                    ) {
                                        continue;
                                    }

                                    core.loggerService().info(
                                            "Ejecutando migración "
                                                    + owner
                                                    + ":"
                                                    + migration.id()
                                    );

                                    try {
                                        migration.migrate(
                                                connection
                                        );

                                        saveMigration(
                                                connection,
                                                owner,
                                                migration.id()
                                        );

                                        count++;

                                    } catch (Exception exception) {
                                        throw new IllegalStateException(
                                                "Falló la migración "
                                                        + owner
                                                        + ":"
                                                        + migration.id(),
                                                exception
                                        );
                                    }
                                }

                                return count;
                            });

            lastExecuted.put(
                    owner,
                    executed
            );

            if (executed > 0) {
                core.loggerService().info(
                        "Migraciones aplicadas para "
                                + owner
                                + ": "
                                + executed
                );
            }

            return executed;

        } finally {
            lock.unlock();
        }
    }

    public int lastExecutedCount() {
        return lastExecutedCount(
                CORE_OWNER
        );
    }

    public int lastExecutedCount(
            String ownerId
    ) {
        return lastExecuted.getOrDefault(
                normalizeOwner(ownerId),
                0
        );
    }

    private void createMigrationTable(
            Connection connection
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement("""
                                CREATE TABLE IF NOT EXISTS ariatus_schema_migrations (
                                    owner_id VARCHAR(64) NOT NULL,
                                    migration_id VARCHAR(128) NOT NULL,
                                    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (owner_id, migration_id)
                                )
                                """)
        ) {
            statement.executeUpdate();
        }
    }

    private boolean hasMigration(
            Connection connection,
            String ownerId,
            String migrationId
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement("""
                                SELECT 1
                                FROM ariatus_schema_migrations
                                WHERE owner_id = ?
                                  AND migration_id = ?
                                LIMIT 1
                                """)
        ) {
            statement.setString(
                    1,
                    ownerId
            );

            statement.setString(
                    2,
                    migrationId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                return resultSet.next();
            }
        }
    }

    private void saveMigration(
            Connection connection,
            String ownerId,
            String migrationId
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement("""
                                INSERT INTO ariatus_schema_migrations
                                (owner_id, migration_id)
                                VALUES (?, ?)
                                """)
        ) {
            statement.setString(
                    1,
                    ownerId
            );

            statement.setString(
                    2,
                    migrationId
            );

            statement.executeUpdate();
        }
    }

    private void validateMigrations(
            Collection<? extends DatabaseMigration> migrations
    ) {
        Set<String> ids =
                new LinkedHashSet<>();

        for (
                DatabaseMigration migration :
                migrations
        ) {
            Objects.requireNonNull(
                    migration,
                    "migration"
            );

            String id =
                    validateMigrationId(
                            migration.id()
                    );

            if (!ids.add(id)) {
                throw new IllegalStateException(
                        "Migración duplicada: " + id
                );
            }
        }
    }

    private String normalizeOwner(
            String value
    ) {
        String owner =
                Objects.requireNonNull(
                                value,
                                "ownerId"
                        )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                owner.isEmpty()
                        || owner.length() > 64
                        || !owner.matches(
                        "^[a-z0-9][a-z0-9_-]*$"
                )
        ) {
            throw new IllegalArgumentException(
                    "Owner de migración inválido: "
                            + value
            );
        }

        return owner;
    }

    private String validateMigrationId(
            String value
    ) {
        String id =
                Objects.requireNonNull(
                                value,
                                "migration.id"
                        )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                id.isEmpty()
                        || id.length() > 128
                        || !id.matches(
                        "^[a-z0-9][a-z0-9._-]*$"
                )
        ) {
            throw new IllegalArgumentException(
                    "ID de migración inválido: "
                            + value
            );
        }

        return id;
    }
}