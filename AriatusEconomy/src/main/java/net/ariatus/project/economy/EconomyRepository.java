package net.ariatus.project.economy;

import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.api.economy.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyRepository {

    CompletableFuture<BigDecimal> balance(UUID uuid, Currency currency);

    CompletableFuture<Void> set(UUID uuid, Currency currency, BigDecimal amount);

    CompletableFuture<Void> log(
            UUID uuid,
            UUID targetUuid,
            Currency currency,
            BigDecimal amount,
            TransactionType type,
            String reason
    );
}