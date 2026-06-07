package net.ariatus.project.storage;

import net.ariatus.project.AriatusEconomy;
import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.api.economy.TransactionType;
import net.ariatus.project.economy.EconomyRepository;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MariaDBEconomyRepository implements EconomyRepository {

    private final AriatusEconomy module;

    public MariaDBEconomyRepository(AriatusEconomy module) {
        this.module = module;
    }

    @Override
    public CompletableFuture<BigDecimal> balance(UUID uuid, Currency currency) {
        return module.database().queryAsync(connection -> {
            ensureBalance(uuid, currency);

            try (var statement = connection.prepareStatement("""
                    SELECT balance
                    FROM ariatus_economy_balances
                    WHERE uuid = ? AND currency = ?
                    """)) {

                statement.setString(1, uuid.toString());
                statement.setString(2, currency.id());

                try (var result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getBigDecimal("balance");
                    }
                }
            }

            return defaultBalance(currency);
        }, defaultBalance(currency));
    }

    @Override
    public CompletableFuture<Void> set(UUID uuid, Currency currency, BigDecimal amount) {
        return module.database().updateAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO ariatus_economy_balances
                    (uuid, currency, balance)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                    """)) {

                statement.setString(1, uuid.toString());
                statement.setString(2, currency.id());
                statement.setBigDecimal(3, amount);

                return statement.executeUpdate();
            }
        }).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> log(
            UUID uuid,
            UUID targetUuid,
            Currency currency,
            BigDecimal amount,
            TransactionType type,
            String reason
    ) {
        if (!module.configBoolean("config.yml", "economy.transaction-log.enabled", true)) {
            return CompletableFuture.completedFuture(null);
        }

        return module.database().updateAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO ariatus_economy_transactions
                    (uuid, target_uuid, currency, amount, type, reason)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {

                statement.setString(1, uuid.toString());

                if (targetUuid == null) {
                    statement.setString(2, null);
                } else {
                    statement.setString(2, targetUuid.toString());
                }

                statement.setString(3, currency.id());
                statement.setBigDecimal(4, amount);
                statement.setString(5, type.name());
                statement.setString(6, reason);

                return statement.executeUpdate();
            }
        }).thenApply(ignored -> null);
    }

    private void ensureBalance(UUID uuid, Currency currency) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     INSERT IGNORE INTO ariatus_economy_balances
                     (uuid, currency, balance)
                     VALUES (?, ?, ?)
                     """)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, currency.id());
            statement.setBigDecimal(3, defaultBalance(currency));

            statement.executeUpdate();
        }
    }

    private BigDecimal defaultBalance(Currency currency) {
        String path = "economy.currencies." + currency.id() + ".default-balance";
        String value = module.configString("config.yml", path, "0.00");

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }
}