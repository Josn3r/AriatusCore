package net.ariatus.project.api.economy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {

    CompletableFuture<BigDecimal> balance(UUID uuid, Currency currency);

    CompletableFuture<Boolean> has(UUID uuid, Currency currency, BigDecimal amount);

    CompletableFuture<Boolean> deposit(UUID uuid, Currency currency, BigDecimal amount, String reason);

    CompletableFuture<Boolean> withdraw(UUID uuid, Currency currency, BigDecimal amount, String reason);

    CompletableFuture<Boolean> set(UUID uuid, Currency currency, BigDecimal amount, String reason);

    CompletableFuture<Boolean> transfer(UUID from, UUID to, Currency currency, BigDecimal amount, String reason);

    String format(Currency currency, BigDecimal amount);
}