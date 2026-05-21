package net.ariatus.project.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.ariatus.project.AriatusCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class DatabaseService {

    private final AriatusCore core;
    private HikariDataSource dataSource;
    private boolean enabled;

    public DatabaseService(AriatusCore core) {
        this.core = core;
    }

    public void connect() {
        enabled = core.configManager().getBoolean("database.enabled", false);

        if (!enabled) {
            core.loggerService().warn("DatabaseService desactivado en config.yml.");
            return;
        }

        String host = core.configManager().getString("database.host", "localhost");
        int port = core.configManager().getInt("database.port", 3306);
        String database = core.configManager().getString("database.database", "ariatus");
        String username = core.configManager().getString("database.username", "root");
        String password = core.configManager().getString("database.password", "");
        int poolSize = core.configManager().getInt("database.pool-size", 10);

        String jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(1);
        config.setPoolName("AriatusCorePool");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            core.loggerService().info("DatabaseService conectado correctamente.");
        } catch (Exception exception) {
            enabled = false;
            core.loggerService().error("No se pudo conectar a la base de datos: " + exception.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("DatabaseService no está conectado.");
        }

        return dataSource.getConnection();
    }

    public CompletableFuture<Void> runAsync(DatabaseTask task) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                task.execute(connection);
            } catch (Exception exception) {
                core.loggerService().error("Error ejecutando tarea SQL async: " + exception.getMessage());
            }
        });
    }

    public boolean isConnected() {
        return enabled && dataSource != null && !dataSource.isClosed();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            core.loggerService().info("DatabaseService cerrado correctamente.");
        }
    }

    public String status() {
        if (!enabled) {
            return "disabled";
        }

        if (isConnected()) {
            return "connected";
        }

        return "disconnected";
    }

    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                return connection.isValid(3);
            } catch (Exception exception) {
                core.loggerService().error("Test de conexión falló: " + exception.getMessage());
                return false;
            }
        });
    }
}