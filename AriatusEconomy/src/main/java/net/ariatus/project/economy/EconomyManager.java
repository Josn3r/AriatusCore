package net.ariatus.project.economy;

import net.ariatus.project.AriatusEconomy;
import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.api.economy.EconomyService;
import net.ariatus.project.api.economy.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyManager implements EconomyService {

    private final AriatusEconomy module;
    private final EconomyRepository repository;

    public EconomyManager(AriatusEconomy module, EconomyRepository repository) {
        this.module = module;
        this.repository = repository;
    }

    @Override
    public CompletableFuture<BigDecimal> balance(UUID uuid, Currency currency) {
        return repository.balance(uuid, currency)
                .thenApply(amount -> normalize(currency, amount));
    }

    @Override
    public CompletableFuture<Boolean> has(UUID uuid, Currency currency, BigDecimal amount) {
        BigDecimal normalized = normalize(currency, amount);

        return balance(uuid, currency).thenApply(balance ->
                balance.compareTo(normalized) >= 0
        );
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID uuid, Currency currency, BigDecimal amount, String reason) {
        BigDecimal normalized = normalize(currency, amount);

        if (invalidCurrencyAmount(currency, amount)) {
            return CompletableFuture.completedFuture(false);
        }

        return balance(uuid, currency).thenCompose(current -> {
            BigDecimal updated = current.add(normalized);

            return repository.set(uuid, currency, updated)
                    .thenCompose(ignored -> repository.log(
                            uuid,
                            null,
                            currency,
                            normalized,
                            TransactionType.DEPOSIT,
                            safeReason(reason)
                    ))
                    .thenApply(ignored -> true);
        });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID uuid, Currency currency, BigDecimal amount, String reason) {
        BigDecimal normalized = normalize(currency, amount);

        if (invalidCurrencyAmount(currency, amount)) {
            return CompletableFuture.completedFuture(false);
        }

        return balance(uuid, currency).thenCompose(current -> {
            if (!allowNegativeBalances() && current.compareTo(normalized) < 0) {
                return CompletableFuture.completedFuture(false);
            }

            BigDecimal updated = current.subtract(normalized);

            return repository.set(uuid, currency, updated)
                    .thenCompose(ignored -> repository.log(
                            uuid,
                            null,
                            currency,
                            normalized,
                            TransactionType.WITHDRAW,
                            safeReason(reason)
                    ))
                    .thenApply(ignored -> true);
        });
    }

    @Override
    public CompletableFuture<Boolean> set(UUID uuid, Currency currency, BigDecimal amount, String reason) {
        BigDecimal normalized = normalize(currency, amount);

        if (currency == Currency.ODRYS && amount.stripTrailingZeros().scale() > 0) {
            return CompletableFuture.completedFuture(false);
        }

        if (!allowNegativeBalances() && normalized.compareTo(BigDecimal.ZERO) < 0) {
            return CompletableFuture.completedFuture(false);
        }


        return repository.set(uuid, currency, normalized)
                .thenCompose(ignored -> repository.log(
                        uuid,
                        null,
                        currency,
                        normalized,
                        TransactionType.SET,
                        safeReason(reason)
                ))
                .thenApply(ignored -> true);
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID from, UUID to, Currency currency, BigDecimal amount, String reason) {
        BigDecimal normalized = normalize(currency, amount);

        if (from.equals(to) || invalidCurrencyAmount(currency, amount)) {
            return CompletableFuture.completedFuture(false);
        }

        return withdraw(from, currency, normalized, "transfer_out:" + safeReason(reason))
                .thenCompose(withdrawSuccess -> {
                    if (!withdrawSuccess) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return deposit(to, currency, normalized, "transfer_in:" + safeReason(reason))
                            .thenCompose(depositSuccess -> {
                                if (!depositSuccess) {
                                    return deposit(from, currency, normalized, "transfer_refund")
                                            .thenApply(ignored -> false);
                                }

                                return repository.log(
                                        from,
                                        to,
                                        currency,
                                        normalized,
                                        TransactionType.TRANSFER,
                                        safeReason(reason)
                                ).thenApply(ignored -> true);
                            });
                });
    }

    @Override
    public String format(Currency currency, BigDecimal amount) {
        BigDecimal normalized = normalize(currency, amount);
        String number = normalized.toPlainString();

        if (currency == Currency.COINS) {
            return currency.symbol() + number;
        }

        return number + " " + currency.symbol();
    }

    private BigDecimal normalize(Currency currency, BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        int decimals = module.configInt(
                "config.yml",
                "economy.currencies." + currency.id() + ".decimals",
                currency == Currency.ODRYS ? 0 : 2
        );

        return amount.setScale(decimals, RoundingMode.DOWN);
    }

    private boolean invalidAmount(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private boolean allowNegativeBalances() {
        return module.configBoolean("config.yml", "economy.safety.allow-negative-balances", false);
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }

        if (reason.length() > 128) {
            return reason.substring(0, 128);
        }

        return reason;
    }

    private boolean invalidCurrencyAmount(Currency currency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        if (currency == Currency.ODRYS && amount.stripTrailingZeros().scale() > 0) {
            return true;
        }

        return false;
    }
}