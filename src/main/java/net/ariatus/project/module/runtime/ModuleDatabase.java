package net.ariatus.project.module.runtime;

import net.ariatus.project.database.DatabaseQuery;
import net.ariatus.project.database.DatabaseService;
import net.ariatus.project.database.DatabaseTask;
import net.ariatus.project.database.migration.DatabaseMigration;
import net.ariatus.project.database.migration.MigrationManager;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.profiler.ProfilerCategory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ModuleDatabase {

    private final AriatusModule module;
    private final DatabaseService database;
    private final MigrationManager migrations;

    public ModuleDatabase(
            AriatusModule module,
            DatabaseService database,
            MigrationManager migrations
    ) {
        this.module =
                Objects.requireNonNull(
                        module,
                        "module"
                );

        this.database =
                Objects.requireNonNull(
                        database,
                        "database"
                );

        this.migrations =
                Objects.requireNonNull(
                        migrations,
                        "migrations"
                );
    }

    public boolean available() {
        return database.isConnected();
    }

    public String status() {
        return database.status();
    }

    public void requireConnected() {
        database.requireConnected();
    }

    public <T> CompletableFuture<T> queryAsync(
            DatabaseQuery<T> query
    ) {
        return profile(
                () -> database.queryAsync(
                        query
                )
        );
    }

    public CompletableFuture<Integer> updateAsync(
            DatabaseQuery<Integer> query
    ) {
        return profile(
                () -> database.updateAsync(
                        query
                )
        );
    }

    public CompletableFuture<Void> executeAsync(
            DatabaseTask task
    ) {
        return profile(
                () -> database.executeAsync(
                        task
                )
        );
    }

    public <T> CompletableFuture<T> transactionAsync(
            DatabaseQuery<T> transaction
    ) {
        return profile(
                () -> database.transactionAsync(
                        transaction
                )
        );
    }

    public CompletableFuture<Void> transactionAsync(
            DatabaseTask transaction
    ) {
        return profile(
                () -> database.transactionAsync(
                        transaction
                )
        );
    }

    public int migrate(
            DatabaseMigration... migrations
    ) {
        requireConnected();

        int executed =
                this.migrations.run(
                        module.id(),
                        migrations
                );

        module.logger()
                .debug(
                        "Migraciones ejecutadas en este arranque: "
                                + executed
                );

        return executed;
    }

    private <T> CompletableFuture<T> profile(
            Supplier<CompletableFuture<T>> operation
    ) {
        long start =
                System.nanoTime();

        CompletableFuture<T> future;

        try {
            future =
                    operation.get();

        } catch (RuntimeException exception) {
            module.core()
                    .profiler()
                    .error(
                            module.id(),
                            ProfilerCategory.DATABASE
                    );

            module.core()
                    .profiler()
                    .record(
                            module.id(),
                            ProfilerCategory.DATABASE,
                            System.nanoTime()
                                    - start,
                            false
                    );

            throw exception;
        }

        return future.whenComplete(
                (result, throwable) -> {
                    module.core()
                            .profiler()
                            .record(
                                    module.id(),
                                    ProfilerCategory.DATABASE,
                                    System.nanoTime()
                                            - start,
                                    false
                            );

                    if (throwable != null) {
                        module.core()
                                .profiler()
                                .error(
                                        module.id(),
                                        ProfilerCategory.DATABASE
                                );
                    }
                }
        );
    }
}