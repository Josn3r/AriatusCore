package net.ariatus.project.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.ariatus.project.AriatusCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DatabaseService {

    private final AriatusCore core;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private volatile HikariDataSource dataSource;
    private volatile ExecutorService executor;
    private volatile boolean enabled;

    public DatabaseService(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
    }

    public synchronized void connect() {
        if (isConnected()) {
            return;
        }

        enabled = core.configManager().getBoolean("database.enabled", false);

        if (!enabled) {
            core.loggerService().warn("Base de datos desactivada en config.yml.");
            return;
        }

        shuttingDown.set(false);

        String host = core.configManager().getString("database.host", "localhost");
        int port = core.configManager().getInt("database.port", 3306);
        String database = core.configManager().getString("database.database", "ariatus");
        String username = core.configManager().getString("database.username", "root");
        String password = core.configManager().getString("database.password", "");

        int poolSize = Math.max(
                1,
                core.configManager().getInt("database.pool-size", 10)
        );

        long connectionTimeout = Math.max(
                1_000L,
                core.configManager().getLong(
                        "database.connection-timeout-ms",
                        10_000L
                )
        );

        long validationTimeout = Math.max(
                250L,
                Math.min(
                        core.configManager().getLong(
                                "database.validation-timeout-ms",
                                5_000L
                        ),
                        connectionTimeout - 250L
                )
        );

        long maxLifetime = Math.max(
                30_000L,
                core.configManager().getLong(
                        "database.max-lifetime-ms",
                        1_800_000L
                )
        );

        String jdbcUrl =
                "jdbc:mariadb://"
                        + host
                        + ":"
                        + port
                        + "/"
                        + database
                        + "?useUnicode=true&characterEncoding=utf8";

        HikariConfig hikari = new HikariConfig();

        hikari.setPoolName("AriatusCorePool");
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");

        hikari.setJdbcUrl(jdbcUrl);
        hikari.setUsername(username);
        hikari.setPassword(password);

        hikari.setMaximumPoolSize(poolSize);

        hikari.setConnectionTimeout(connectionTimeout);
        hikari.setValidationTimeout(validationTimeout);
        hikari.setMaxLifetime(maxLifetime);

        hikari.setAutoCommit(true);
        hikari.setInitializationFailTimeout(connectionTimeout);

        HikariDataSource createdDataSource = null;

        try {
            createdDataSource = new HikariDataSource(hikari);

            try (Connection connection = createdDataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    throw new SQLException(
                            "MariaDB no respondió correctamente al test de conexión."
                    );
                }
            }

            ExecutorService createdExecutor =
                    Executors.newThreadPerTaskExecutor(
                            Thread.ofVirtual()
                                    .name("Ariatus-DB-", 0)
                                    .factory()
                    );

            dataSource = createdDataSource;
            executor = createdExecutor;

            core.loggerService().info(
                    "DatabaseService conectado. Pool máximo: "
                            + poolSize
                            + "."
            );

        } catch (Exception exception) {
            enabled = false;

            if (createdDataSource != null) {
                createdDataSource.close();
            }

            throw new DatabaseException(
                    "No se pudo conectar AriatusCore a MariaDB.",
                    exception
            );
        }
    }

    public Connection getConnection() throws SQLException {
        requireConnected();

        return dataSource.getConnection();
    }

    public void requireConnected() {
        if (!isConnected()) {
            throw new DatabaseException(
                    enabled
                            ? "DatabaseService no está conectado."
                            : "DatabaseService está desactivado."
            );
        }

        if (shuttingDown.get()) {
            throw new DatabaseException(
                    "DatabaseService se está apagando."
            );
        }
    }

    public <T> CompletableFuture<T> queryAsync(DatabaseQuery<T> query) {
        return submit(query);
    }

    public CompletableFuture<Integer> updateAsync(DatabaseQuery<Integer> query) {
        return submit(query);
    }

    public CompletableFuture<Void> executeAsync(DatabaseTask task) {
        Objects.requireNonNull(task, "task");

        return submit(connection -> {
            task.execute(connection);
            return null;
        });
    }

    public <T> CompletableFuture<T> transactionAsync(DatabaseQuery<T> transaction) {
        Objects.requireNonNull(transaction, "transaction");

        return submit(connection ->
                executeTransaction(
                        connection,
                        transaction
                )
        );
    }

    public CompletableFuture<Void> transactionAsync(DatabaseTask transaction) {
        Objects.requireNonNull(transaction, "transaction");

        return submit(connection ->
                executeTransaction(
                        connection,
                        currentConnection -> {
                            transaction.execute(currentConnection);
                            return null;
                        }
                )
        );
    }

    public <T> T executeBlocking(DatabaseQuery<T> query) {
        Objects.requireNonNull(query, "query");

        requireConnected();

        try (Connection connection = getConnection()) {
            return query.execute(connection);

        } catch (DatabaseException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new DatabaseException(
                    "Error ejecutando operación SQL.",
                    exception
            );
        }
    }

    public void executeBlocking(DatabaseTask task) {
        Objects.requireNonNull(task, "task");

        executeBlocking(connection -> {
            task.execute(connection);
            return null;
        });
    }

    public CompletableFuture<Boolean> testConnection() {
        if (!isConnected()) {
            return CompletableFuture.completedFuture(false);
        }

        return queryAsync(
                connection -> connection.isValid(3)
        ).exceptionally(exception -> false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConnected() {
        HikariDataSource current = dataSource;

        return enabled
                && current != null
                && !current.isClosed()
                && !shuttingDown.get();
    }

    public String status() {
        if (!enabled) {
            return "disabled";
        }

        return isConnected()
                ? "connected"
                : "disconnected";
    }

    public synchronized void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }

        ExecutorService currentExecutor = executor;
        HikariDataSource currentDataSource = dataSource;

        executor = null;
        dataSource = null;

        if (currentExecutor != null) {
            currentExecutor.shutdown();

            try {
                if (!currentExecutor.awaitTermination(
                        10,
                        TimeUnit.SECONDS
                )) {
                    currentExecutor.shutdownNow();
                }

            } catch (InterruptedException exception) {
                currentExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (
                currentDataSource != null
                        && !currentDataSource.isClosed()
        ) {
            currentDataSource.close();
        }

        core.loggerService().info(
                "DatabaseService cerrado correctamente."
        );
    }

    private <T> CompletableFuture<T> submit(DatabaseQuery<T> query) {
        Objects.requireNonNull(query, "query");

        try {
            requireConnected();

            ExecutorService currentExecutor = executor;

            if (currentExecutor == null) {
                return CompletableFuture.failedFuture(
                        new DatabaseException(
                                "El executor de DatabaseService no está disponible."
                        )
                );
            }

            return CompletableFuture.supplyAsync(
                    () -> executeBlocking(query),
                    currentExecutor
            );

        } catch (
                DatabaseException
                | RejectedExecutionException exception
        ) {
            return CompletableFuture.failedFuture(
                    exception
            );
        }
    }

    private <T> T executeTransaction(
            Connection connection,
            DatabaseQuery<T> transaction
    ) throws Exception {

        boolean previousAutoCommit =
                connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            T result =
                    transaction.execute(connection);

            connection.commit();

            return result;

        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(
                        rollbackException
                );
            }

            throw exception;

        } finally {
            try {
                connection.setAutoCommit(
                        previousAutoCommit
                );
            } catch (SQLException ignored) {
            }
        }
    }
}